# AI Survey Result Generation Design

## Goal

Create a Johari-window result from completed survey responses. The system must generate a short interpretation and a private-S3-backed image for each quadrant while allowing failed work to resume at the smallest possible unit.

## Confirmed Decisions

- Every SELF and PEER submission receives eight random active questions: two per each of the four `trait_code` values.
- Raw question and answer snapshots are sent to OpenAI for result generation.
- `gpt-5.4-mini` extracts adjectives per question-answer pair, then assigns those adjectives to Johari quadrants and produces each quadrant's interpretation and image prompt.
- `gpt-image-2` generates one image per quadrant from the prepared prompt.
- Intermediate adjective extractions are stored internally, but are never exposed by the public result API.
- Images are stored in private S3. The public API creates a 24-hour presigned URL per image at read time.
- The initial image quality is `low`, configured so it can be changed to `medium` without a code change.
- Each unit of work retries at most three times. Successful work is not repeated; only the failed unit retries.
- A result is `READY` only when every quadrant has an interpretation and an uploaded image. It is `FAILED` only when a required unit exhausts its retries.
- OpenAI integration uses the official OpenAI Java SDK in infrastructure, not Spring AI. The existing core port remains the provider boundary.

## Architecture

`ResultGenerationService` remains the scheduler-facing orchestrator in core. It depends only on these core ports:

- `ResultGenerationSourceReader`: reads completed SELF/PEER question-answer snapshots, including trait code.
- `ResultNarrativeClient`: calls `gpt-5.4-mini` for adjective extraction, quadrant classification, interpretations, and image prompts.
- `ResultImageClient`: calls `gpt-image-2` for a single quadrant image.
- `ResultImageStorage`: writes a generated image to private S3 and returns its object key.
- `ResultUrlSigner`: turns a stored object key into a 24-hour presigned URL for result reads.

Infrastructure implements the ports with the OpenAI Java SDK and AWS S3 SDK. `looky-api` provides configuration beans and never imports infrastructure classes directly.

## Question Assignment

Question selection happens when each submission starts, not when the survey is created.

1. The core question contract contains the question's `trait_code`.
2. The infrastructure query fetches active questions for each of the four supported traits.
3. Two questions are selected randomly from each trait group.
4. The combined eight questions are assigned to the new SELF or PEER submission.
5. If any trait has fewer than two active questions, submission start fails with `NOT_ENOUGH_ACTIVE_QUESTIONS`.

## Generation Data Flow

1. When SELF plus three PEER submissions are complete and the result-open time has passed, the scheduler loads their question-answer snapshots.
2. `ResultNarrativeClient` extracts Johari adjectives for every question-answer pair and persists those internal records.
3. The same client uses the complete adjective set to assign quadrants and produce four interpretations and four image prompts.
4. Each quadrant is generated independently through `ResultImageClient` and uploaded through `ResultImageStorage`.
5. `result_quadrants` stores its interpretation, image prompt, private S3 object key, status, attempt count, and failure metadata.
6. Result read signs only completed image keys and returns them alongside the corresponding interpretation.

## State and Retry Rules

The survey-level result status remains `GENERATING`, `READY`, or `FAILED` once prerequisites are met.

- `GENERATING`: at least one adjective extraction, narrative, image generation, or upload is pending or retryable.
- `READY`: all four quadrants have completed interpretation and image uploads.
- `FAILED`: a required unit has exhausted three attempts.

Each stored unit has its own status and attempt count. Retrying a failed quadrant must not call OpenAI or S3 for a successful quadrant. Retry limits are configured under `looky.result-generation.max-attempts`, with a default of `3`.

## Storage and Configuration

- S3 object key: `surveys/{surveyCode}/results/{quadrant}.png`.
- S3 objects are private.
- Presigned URL expiration: 24 hours.
- Image quality property: `looky.result-generation.image-quality`, default `low`.
- Model properties: `looky.result-generation.narrative-model=gpt-5.4-mini` and `looky.result-generation.image-model=gpt-image-2`.
- Credentials and bucket settings are supplied only through deployment configuration; neither API keys nor AWS credentials are committed.

## Public API Contract

`GET /api/v1/surveys/{surveyCode}/result` keeps the existing common response wrapper and `quadrantImageUrls` field. In `READY`, it adds `quadrantInterpretations`.

```json
{
  "status": "success",
  "message": "설문 결과를 조회했습니다.",
  "payload": {
    "surveyCode": "b91k2p8xq4z2",
    "resultStatus": "READY",
    "quadrantImageUrls": {
      "OPEN": "https://private-bucket.s3...presigned",
      "BLIND": "https://private-bucket.s3...presigned",
      "HIDDEN": "https://private-bucket.s3...presigned",
      "UNKNOWN": "https://private-bucket.s3...presigned"
    },
    "quadrantInterpretations": {
      "OPEN": "...",
      "BLIND": "...",
      "HIDDEN": "...",
      "UNKNOWN": "..."
    }
  }
}
```

For every non-`READY` result, both maps are `null`. Adjective records and model-classification evidence are never returned. Swagger stays on `SurveyApi`; the existing `ApiResponse` wrapper is unchanged.

## Error Handling

- Invalid or malformed model output fails only the affected work unit and records an internal failure reason.
- OpenAI or S3 transient errors retry the failed unit through the existing scheduler.
- A missing stored object during result signing is an internal server error and does not expose S3 details.
- Logs include survey ID, unit type, quadrant when relevant, and attempt count. Raw question/answer text and API credentials must not be logged.

## Testing

- Unit tests for exactly two randomly selected active questions per trait and for insufficient trait inventory.
- Unit tests for structured adjective/narrative parsing and invalid model output.
- Unit tests for failed-unit-only retries and retry exhaustion.
- Infrastructure tests for private S3 upload key construction and 24-hour URL signing.
- API tests for `quadrantInterpretations`, `quadrantImageUrls`, and unchanged common response structure.
- Integration tests covering balanced response assignment, persisted adjective records, one failed quadrant followed by successful retry, and final `READY` response.

## Out of Scope

- A public or unauthenticated manual regeneration endpoint.
- Exposing raw responses, adjectives, or model reasoning to clients.
- Public S3 objects or permanent image URLs.
