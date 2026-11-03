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

    /** Grava o novo papel e devolve o perfil atualizado, ou vazio se o id não existir. */
    Optional<Perfil> atualizarPapel(UUID id, Papel papel);
}
