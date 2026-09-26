# `javax` → `jakarta`

The reason a great many systems are stuck on Java 8/11. It *looks* like a package rename and
is not: every dependency needs a Jakarta build, and automated renamers miss the names that
live **inside strings**.

## Plan

1. **OpenRewrite** recipe `org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta`
   (run via the Maven plugin) to rewrite imports and type references.
2. Bump dependencies to their Jakarta EE 9+ builds (Hibernate 6, JAX-RS 3, etc.).
3. **Hand-fix what the recipe cannot see** — the string literals:
   - [ ] `persistence.xml` (`persistence-unit`, provider class names)
   - [ ] `Class.forName("javax...")` reflective lookups
   - [ ] Spring/JAXB/logging XML config referencing `javax.*`
   - [ ] property keys/values in `.properties` / `.yml`
4. Re-run the **characterisation tests** — the rename must not change behaviour.

## Command (to be added)

```bash
mvn -N org.openrewrite.maven:rewrite-maven-plugin:run \
  -Drewrite.activeRecipes=org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta
```
