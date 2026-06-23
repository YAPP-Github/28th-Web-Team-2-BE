# Production Environment Variables

The production container must run with `SPRING_PROFILES_ACTIVE=prod`.

## Required

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `OPENAI_API_KEY`
- `LOOKY_RESULT_S3_BUCKET`
- `AWS_REGION`

## Optional

- `DB_DRIVER_CLASS_NAME`

If `DB_DRIVER_CLASS_NAME` is omitted, the application uses `com.mysql.cj.jdbc.Driver`.
