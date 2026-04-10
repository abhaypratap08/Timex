# Timex

Timex is a small Java Swing clock project with two variants:

- `DIGITAL_TIMEX/` for the digital clock and Pomodoro timer
- `ANALOG_TIMEX/` for the analog clock

## Project structure

- `src/` contains the Java source
- `dist/` contains the packaged runnable JAR

## Installation

Timex requires Java to be installed.

Recommended:

- JDK 21 or newer if you want to compile from source
- a Java runtime if you only want to launch the packaged JARs

Check your installation:

```bash
java -version
```

If you want to compile from source, also check:

```bash
javac -version
```

## Run the packaged apps

Digital Timex:

```bash
java -jar DIGITAL_TIMEX/dist/Timex.jar
```

Analog Timex:

```bash
java -jar ANALOG_TIMEX/dist/Analog.jar
```

## GitHub release workflow

This repository includes a GitHub Actions workflow at `.github/workflows/release.yml`.

What it does:

- builds both JARs from source with Java 21
- recreates `DIGITAL_TIMEX/dist/Timex.jar`
- recreates `ANALOG_TIMEX/dist/Analog.jar`
- uploads both JARs as workflow artifacts
- publishes both JARs to a GitHub Release when you push a tag like `v1.0.0`

To create a release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

You can also trigger the workflow manually from the GitHub Actions tab.

## Compile and run from source

Digital Timex:

```bash
javac DIGITAL_TIMEX/src/Timex.java
java -cp DIGITAL_TIMEX/src Timex
```

Analog Timex:

```bash
javac ANALOG_TIMEX/src/Analog.java
java -cp ANALOG_TIMEX/src Analog
```

## Notes

- The digital build includes a Pomodoro timer with editable time fields.
- Both variants are desktop Swing apps and open as floating utility-style windows.
- If double-click launching a JAR does not work on your system, run it from the terminal with the commands above.
