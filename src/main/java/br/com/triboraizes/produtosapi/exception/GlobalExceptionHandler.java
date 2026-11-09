package br.com.triboraizes.produtosapi.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
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

    @ExceptionHandler(EstoqueInsuficienteException.class)
    public ProblemDetail handleEstoqueInsuficiente(EstoqueInsuficienteException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        problem.setTitle("Estoque insuficiente");
        return problem;
    }

    @ExceptionHandler(ConflitoDeVersaoException.class)
    public ProblemDetail handleConflitoDeVersao(ConflitoDeVersaoException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflito de edição");
        return problem;
    }

    @ExceptionHandler(CodigoDeProdutoDuplicadoException.class)
    public ProblemDetail handleCodigoDuplicado(CodigoDeProdutoDuplicadoException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Código já cadastrado");
        return problem;
    }

    @ExceptionHandler(RegistroNaoEncontradoException.class)
    public ProblemDetail handleRegistroNaoEncontrado(RegistroNaoEncontradoException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle(ex.titulo());
        return problem;
    }

    @ExceptionHandler(ConflitoDeDadosException.class)
    public ProblemDetail handleConflitoDeDados(ConflitoDeDadosException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle(ex.titulo());
        return problem;
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ProblemDetail handleRequisicaoInvalida(RequisicaoInvalidaException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Parâmetro inválido");
        return problem;
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ProblemDetail handleCredenciaisInvalidas(CredenciaisInvalidasException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Credenciais inválidas");
        return problem;
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ProblemDetail handleEmailJaCadastrado(EmailJaCadastradoException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("E-mail já cadastrado");
        return problem;
    }

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public ProblemDetail handleUsuarioNaoEncontrado(UsuarioNaoEncontradoException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Usuário não encontrado");
        return problem;
    }

    @ExceptionHandler(OperacaoNaoPermitidaException.class)
    public ProblemDetail handleOperacaoNaoPermitida(OperacaoNaoPermitidaException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        problem.setTitle("Operação não permitida");
        return problem;
    }

    @ExceptionHandler(ServicoDeUsuariosIndisponivelException.class)
    public ProblemDetail handleServicoDeUsuariosIndisponivel(ServicoDeUsuariosIndisponivelException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setTitle("Serviço de usuários indisponível");
        return problem;
    }

    @ExceptionHandler(ServicoDeDadosIndisponivelException.class)
    public ProblemDetail handleServicoDeDadosIndisponivel(ServicoDeDadosIndisponivelException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setTitle("Serviço de dados indisponível");
        return problem;
    }

    @ExceptionHandler(DemandaNaoEncontradaException.class)
    public ProblemDetail handleDemandaNaoEncontrada(DemandaNaoEncontradaException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Demanda não encontrada");
        return problem;
    }

    @ExceptionHandler(ContaNaoEncontradaException.class)
    public ProblemDetail handleContaNaoEncontrada(ContaNaoEncontradaException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Conta não encontrada");
        return problem;
    }

    /** Chamado pelo Spring Security (via {@code SecurityConfig}) quando falta o token ou ele é inválido. */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleNaoAutenticado(AuthenticationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Faça login e envie o token no cabeçalho Authorization: Bearer <token>");
        problem.setTitle("Não autenticado");
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAcessoNegado(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "Seu usuário não tem permissão para esta operação. Peça a um administrador para liberar o acesso.");
        problem.setTitle("Acesso negado");
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
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex,
                                                                          HttpHeaders headers,
                                                                          HttpStatusCode status,
                                                                          WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "A imagem deve ter no máximo 2 MB");
        problem.setTitle("Arquivo muito grande");
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
