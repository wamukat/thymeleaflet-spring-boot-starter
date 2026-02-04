# Thymeleaflet Sample

This is a sample app demonstrating Thymeleaflet with DaisyUI.

## Setup (for users)

1) Add the starter dependency (from Maven Central):

```xml
<dependency>
  <groupId>io.github.wamukat</groupId>
  <artifactId>thymeleaflet-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

2) Build the sample CSS (DaisyUI + Tailwind) from this directory:

```bash
npm install
npm run build
```

3) Run the sample app from this directory:

```bash
mvn spring-boot:run
```

4) Open:

```
http://localhost:6006/
```

Form samples:

```
http://localhost:6006/forms
```

## Setup (for contributors)

If you want to test the latest starter code locally:

1) Build and install the starter to your local Maven repo (from repo root):

```bash
./mvnw -DskipTests install
```

2) Build the sample CSS (DaisyUI + Tailwind) from this directory:

```bash
npm install
npm run build
```

3) Run the sample app from this directory:

```bash
mvn spring-boot:run
```

## Notes

- The main page (`/`) uses the same fragments you can preview in Thymeleaflet.
- Fragment previews are available at:
  - `http://localhost:6006/thymeleaflet`
- CSS output files are generated to:
  - `src/main/resources/static/css/mypage.css`
  - `src/main/resources/static/css/mypage/components.css`
- Stories are placed under:
  - `src/main/resources/META-INF/thymeleaflet/stories/`
- Thymeleaflet is a development tool. For production, disable it via
  `spring.autoconfigure.exclude` (see `src/main/resources/application-prod.yml`).

## Screenshot Data

Dummy data for screenshots is included in:

- `templates/components/profile-card.html`
- `META-INF/thymeleaflet/stories/components/profile-card.stories.yml`
