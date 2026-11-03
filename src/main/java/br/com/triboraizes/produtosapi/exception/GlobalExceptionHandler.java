package br.com.triboraizes.produtosapi.exception;

import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

/**
 * Converte as exceções da aplicação em respostas no formato RFC 9457 (ProblemDetail).
 * Exceções padrão do Spring MVC (405, 415, rota inexistente etc.) são tratadas pela classe base.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record CampoInvalido(String campo, String mensagem) {
    }

    @ExceptionHandler(ProdutoNaoEncontradoException.class)
    public ProblemDetail handleProdutoNaoEncontrado(ProdutoNaoEncontradoException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Produto não encontrado");
        return problem;
    }

    @ExceptionHandler(ConflitoDeVersaoException.class)
    public ProblemDetail handleConflitoDeVersao(ConflitoDeVersaoException ex) {
        return conflitoDeEdicao(ex.getMessage());
    }

    /** Duas transações alteraram o mesmo produto ao mesmo tempo e a segunda perdeu a disputa. */
    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    public ProblemDetail handleEdicaoSimultanea(RuntimeException ex) {
        return conflitoDeEdicao("O produto foi alterado por outra pessoa. Recarregue os dados e tente novamente.");
    }

    private static ProblemDetail conflitoDeEdicao(String detalhe) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detalhe);
        problem.setTitle("Conflito de edição");
        return problem;
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ProblemDetail handleRequisicaoInvalida(RequisicaoInvalidaException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Parâmetro inválido");
        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTipoDeParametroInvalido(MethodArgumentTypeMismatchException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "O parâmetro '" + ex.getName() + "' possui um valor inválido");
        problem.setTitle("Parâmetro inválido");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleErroInesperado(Exception ex) {
        log.error("Erro inesperado ao processar a requisição", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. Tente novamente mais tarde.");
        problem.setTitle("Erro interno");
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<CampoInvalido> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new CampoInvalido(erro.getField(), erro.getDefaultMessage()))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Um ou mais campos são inválidos");
        problem.setTitle("Dados inválidos");
        problem.setProperty("erros", erros);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status,
                "O corpo da requisição está ausente ou malformado");
        problem.setTitle("Requisição inválida");
        return handleExceptionInternal(ex, problem, headers, status, request);
    }
}
