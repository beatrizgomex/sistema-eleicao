package br.com.eleicao.sistema.votacao.dto;

import java.time.LocalDateTime;

public record ComprovanteResponse(String codigo, Long eleicaoId, LocalDateTime emitidoEm) {}
