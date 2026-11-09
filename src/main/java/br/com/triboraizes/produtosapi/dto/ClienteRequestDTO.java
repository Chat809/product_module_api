package br.com.triboraizes.produtosapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Cadastro de cliente")
public record ClienteRequestDTO(
        @Schema(description = "PF (pessoa física) ou PJ (pessoa jurídica); padrão: PF", example = "PF")
        @Pattern(regexp = "^(PF|PJ)?$", message = "O tipo deve ser PF ou PJ")
        String tipo,

        @Schema(example = "Maria Souza")
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 150, message = "O nome deve ter entre 2 e 150 caracteres")
        String nome,

        @Schema(description = "CPF (11 dígitos) ou CNPJ (14 dígitos); pontuação é ignorada")
        @Pattern(regexp = "^\\s*$|^[\\d./\\-\\s]{11,20}$", message = "Informe um CPF ou CNPJ válido")
        String documento,

        @Email(message = "Informe um e-mail válido")
        @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres")
        String email,

        @Size(max = 30, message = "O telefone deve ter no máximo 30 caracteres")
        String telefone,

        @Past(message = "A data de nascimento deve estar no passado")
        LocalDate dataNascimento,

        @Pattern(regexp = "^\\s*$|^\\d{5}-?\\d{3}$", message = "Informe um CEP válido")
        String cep,

        @Size(max = 200, message = "O endereço deve ter no máximo 200 caracteres")
        String endereco,

        @Size(max = 80, message = "A cidade deve ter no máximo 80 caracteres")
        String cidade,

        @Pattern(regexp = "^\\s*$|^[A-Za-z]{2}$", message = "Informe a UF com 2 letras")
        String uf,

        @Size(max = 60, message = "A origem deve ter no máximo 60 caracteres")
        String origem,

        UUID responsavelId,

        @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres")
        String observacoes,

        @Schema(description = "Padrão: true")
        Boolean ativo
) {
}
