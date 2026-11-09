package br.com.triboraizes.produtosapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Cadastro de fornecedor")
public record FornecedorRequestDTO(
        @Schema(example = "Distribuidora Raízes Ltda")
        @NotBlank(message = "A razão social é obrigatória")
        @Size(min = 2, max = 150, message = "A razão social deve ter entre 2 e 150 caracteres")
        String razaoSocial,

        @Size(max = 150, message = "O nome fantasia deve ter no máximo 150 caracteres")
        String nomeFantasia,

        @Schema(description = "CPF (11 dígitos) ou CNPJ (14 dígitos); pontuação é ignorada", example = "12.345.678/0001-90")
        @Pattern(regexp = "^\\s*$|^[\\d./\\-\\s]{11,20}$", message = "Informe um CPF ou CNPJ válido")
        String documento,

        @Size(max = 100, message = "O contato deve ter no máximo 100 caracteres")
        String contato,

        @Email(message = "Informe um e-mail válido")
        @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres")
        String email,

        @Size(max = 30, message = "O telefone deve ter no máximo 30 caracteres")
        String telefone,

        @Size(max = 140, message = "A chave Pix deve ter no máximo 140 caracteres")
        String chavePix,

        @Size(max = 60, message = "A categoria deve ter no máximo 60 caracteres")
        String categoria,

        @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres")
        String observacoes,

        @Schema(description = "Padrão: true")
        Boolean ativo
) {
}
