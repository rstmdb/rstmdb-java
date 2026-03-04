# Test Standards

## BDD Style Testing

### Assertions

- **Always use `BDDAssertions`** from AssertJ instead of standard assertions
- Import: `import static org.assertj.core.api.BDDAssertions.*;`
- Use `then()` instead of `assertThat()`

### Mocking

- **Always use `BDDMockito`** instead of standard Mockito
- Import: `import static org.mockito.BDDMockito.*;`
- Use `given()` instead of `when()` for stubbing
- Use `then()` for verification

### Method Name Conflicts

When there's a clash between method names (e.g., `then()` exists in both BDDAssertions and BDDMockito):

- **Always use fully qualified class names** to avoid ambiguity
- Example: `BDDAssertions.then(result).isEqualTo(expected);`
- Example: `BDDMockito.then(mock).should().someMethod();`

### Test Structure

- **All tests must be split into 3 sections** with comments:
  - `// given` - Setup and preconditions
  - `// when` - Action being tested
  - `// then` - Assertions and verification

### Test Naming

- **All tests must be annotated with `@DisplayName`**
- Format: `@DisplayName("When <condition> Then <expected behavior>")`
- Example: `@DisplayName("When order is paid Then state transitions to paid")`

### Example

```java
import static org.assertj.core.api.BDDAssertions.*;
import static org.mockito.BDDMockito.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MyServiceTest {
    @Test
    @DisplayName("When order exists Then process successfully")
    void shouldProcessOrder() {
        // given
        var repository = mock(OrderRepository.class);
        given(repository.findById("123")).willReturn(Optional.of(order));
        
        // when
        var result = service.process("123");
        
        // then
        BDDAssertions.then(result.getStatus()).isEqualTo("processed");
        BDDMockito.then(repository).should().save(any());
    }
}
```
