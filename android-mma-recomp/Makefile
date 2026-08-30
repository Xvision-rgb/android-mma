.PHONY: test lint

# Logique métier JVM (JUnit). Nécessite le SDK Android / Gradle wrapper
# du module — pas disponible dans tous les environnements CI légers.
test:
	./gradlew test --offline || ./gradlew test

lint:
	./gradlew lint || true
