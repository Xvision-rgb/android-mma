.PHONY: bootstrap lint test check

bootstrap:
	@test -f app/src/main/java/com/example/mmarecomp/SupabaseConfig.kt || \
		cp app/src/main/java/com/example/mmarecomp/SupabaseConfig.kt.example \
		   app/src/main/java/com/example/mmarecomp/SupabaseConfig.kt

lint: bootstrap
	./gradlew lintDebug

test: bootstrap
	./gradlew testDebugUnitTest

check: lint test
