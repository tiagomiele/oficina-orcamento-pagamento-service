package br.com.oficina.orcamentopagamento.adapter.controller;

import br.com.oficina.orcamentopagamento.domain.model.TransicaoInvalidaException;
import br.com.oficina.orcamentopagamento.usecase.AcessoNegadoException;
import br.com.oficina.orcamentopagamento.usecase.OrcamentoNaoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(OrcamentoNaoEncontradoException.class)
  ProblemDetail naoEncontrado(OrcamentoNaoEncontradoException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(AcessoNegadoException.class)
  ProblemDetail acessoNegado(AcessoNegadoException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
  }

  @ExceptionHandler(TransicaoInvalidaException.class)
  ProblemDetail conflito(TransicaoInvalidaException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
  }
}
