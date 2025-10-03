FROM eclipse-temurin:17-jdk AS build
COPY . /app
WORKDIR /app
RUN chmod +x ./gradlew
RUN ./gradlew bootJar || gradle bootJar
RUN mv -f build/libs/*.jar app.jar

FROM eclipse-temurin:17-jre
ARG PORT
ENV PORT=${PORT}
COPY --from=build /app/app.jar .
RUN useradd runtime
USER runtime
ENTRYPOINT [ "java", "-Xms256m", "-Xmx512m", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", "-XX:+UseStringDeduplication", "-XX:+UseCompressedOops", "-XX:+UseCompressedClassPointers", "-XX:+TieredCompilation", "-XX:TieredStopAtLevel=1", "-XX:+DisableExplicitGC", "-Djava.awt.headless=true", "-Dfile.encoding=UTF-8", "-Duser.timezone=UTC", "-Dserver.port=${PORT}", "-jar", "app.jar" ]
