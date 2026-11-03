package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.model.Papel;
import br.com.triboraizes.produtosapi.model.Perfil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Contrato de acesso aos perfis de usuário, independente de onde estejam guardados. */
public interface PerfilDAO {

    Optional<Perfil> buscarPorId(UUID id);

    /** Lista uma página de perfis ordenada por nome. */
    List<Perfil> listar(int pagina, int tamanho);

    long contar();

    /** Grava os dados editáveis e devolve o perfil atualizado, ou vazio se o id não existir. */
    Optional<Perfil> atualizar(UUID id, String nome, String email, Papel papel, List<String> acessos);

    /** Grava os dados que o próprio usuário pode editar em "Meu perfil". */
    Optional<Perfil> atualizarDadosProprios(UUID id, String nome, String telefone, String funcao, String bio);

    /** Grava ou limpa ({@code null}) o endereço público do avatar. */
    Optional<Perfil> atualizarAvatar(UUID id, String avatarUrl);

    /** Todos os perfis, só com id, nome e avatar, em ordem alfabética (para escolher responsáveis). */
    List<Perfil> listarOpcoes();
}
