package br.com.eleicao.sistema.shared.config;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.stream.Collectors;

@Hidden
@RestControllerAdvice
public class TratadorExcecoesApi {

    public record ErroApiResponse(
            Instant timestamp,
            int status,
            String error,
            String message,
            String path
    ) {
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroApiResponse> tratarResponseStatus(
            ResponseStatusException excecao,
            HttpServletRequest requisicao) {

        HttpStatusCode statusCode = excecao.getStatusCode();
        String descricaoStatus = (statusCode instanceof HttpStatus httpStatus)
                ? httpStatus.getReasonPhrase()
                : statusCode.toString();
        String mensagem = excecao.getReason() != null ? excecao.getReason() : descricaoStatus;

        ErroApiResponse corpo = new ErroApiResponse(
                Instant.now(),
                statusCode.value(),
                descricaoStatus,
                mensagem,
                requisicao.getRequestURI()
        );

        return ResponseEntity.status(statusCode)
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroApiResponse> tratarValidacao(
            MethodArgumentNotValidException excecao,
            HttpServletRequest requisicao) {

        String mensagem = excecao.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(texto -> texto != null && !texto.isBlank())
                .distinct()
                .collect(Collectors.joining("; "));

        if (mensagem.isBlank()) {
            mensagem = "Dados de entrada inválidos";
        }

        ErroApiResponse corpo = new ErroApiResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                mensagem,
                requisicao.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo);
    }
}
