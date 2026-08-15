package vn.coreplatform.shared;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(ApiProblem.class)
  ResponseEntity<ProblemDetail> handle(ApiProblem error, HttpServletRequest request) {
    var problem = ProblemDetail.forStatusAndDetail(error.status, error.getMessage());
    problem.setTitle(error.code); problem.setType(URI.create("https://core.local/problems/" + error.code));
    problem.setProperty("code", error.code); problem.setProperty("timestamp", Instant.now());
    problem.setProperty("path", request.getRequestURI());
    return ResponseEntity.status(error.status).body(problem);
  }
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException error) {
    var p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ"); p.setTitle("VALIDATION_FAILED");
    p.setProperty("errors", error.getBindingResult().getFieldErrors().stream().map(e -> Map.of("field", e.getField(), "message", e.getDefaultMessage())).toList());
    return ResponseEntity.badRequest().body(p);
  }
  public static class ApiProblem extends RuntimeException {
    final HttpStatus status; final String code;
    public ApiProblem(HttpStatus status, String code, String message) { super(message); this.status=status; this.code=code; }
  }
}
