package br.com.sankhya.truss.regraunidademedida;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;

import java.math.BigDecimal;

public class regraUnidadeEvent implements EventoProgramavelJava {

    /**
     * Evento executado antes da inserção de um item de nota.
     * Verifica se o item está sendo inserido com unidade diferente da padrão
     * e, em alguns casos específicos, força a unidade padrão do produto.
     */
    @Override
    public void beforeInsert(PersistenceEvent event) throws Exception {
        DynamicVO newVO = (DynamicVO) event.getVo();

        // Recupera os dados do cabeçalho da nota
        JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
        DynamicVO cabVO = cabDAO.findByPK(newVO.asBigDecimal("NUNOTA"));

        BigDecimal codparc = cabVO.asBigDecimal("CODPARC");         // Código do parceiro
        BigDecimal codtipoper = cabVO.asBigDecimal("CODTIPOPER");   // Código da TOP
        BigDecimal codprod = newVO.asBigDecimal("CODPROD");         // Código do produto
        String codvol = newVO.asString("CODVOL");                   // Unidade informada na nota

        // Valida se: a TOP é específica, o parceiro está parametrizado, e a unidade é diferente da padrão
        if (validaTop(codtipoper) && validaParceiro(codparc) && validaUnidade(codprod, codvol)) {
            // Recupera a unidade padrão do produto e força essa unidade no item da nota
            JapeWrapper proDAO = JapeFactory.dao(DynamicEntityNames.PRODUTO);
            DynamicVO proVO = proDAO.findByPK(codprod);
            String unidadePadrao = proVO.asString("CODVOL");

            // Substitui a unidade da nota pela unidade padrão do produto
            newVO.setProperty("CODVOL", unidadePadrao);
        }
    }


    @Override public void beforeUpdate(PersistenceEvent event) throws Exception {}
    @Override public void beforeDelete(PersistenceEvent event) throws Exception {}
    @Override public void afterInsert(PersistenceEvent event) throws Exception {}
    @Override public void afterUpdate(PersistenceEvent event) throws Exception {}
    @Override public void afterDelete(PersistenceEvent event) throws Exception {}
    @Override public void beforeCommit(TransactionContext event) throws Exception {}

    /**
     * Verifica se a TOP  está parametrizada na central de parâmetros, no parâmetro 5.
     */
    private boolean validaTop(BigDecimal codtipoper) throws Exception {
        JapeWrapper centralTopDAO = JapeFactory.dao("AD_CENTRALPARAMTOP");
        DynamicVO centralTopVO = centralTopDAO.findByPK(BigDecimal.valueOf(5), codtipoper);

        return centralTopVO != null;
    }

    /**
     * Verifica se o parceiro está parametrizado na central de parâmetros, no parâmetro 5.
     */
    private boolean validaParceiro(BigDecimal codparc) throws Exception {
        JapeWrapper centralParDAO = JapeFactory.dao("AD_CENTRALPARAMPAR");
        DynamicVO centralParVO = centralParDAO.findByPK(BigDecimal.valueOf(5), codparc);

        return centralParVO != null;
    }

    /**
     * Compara a unidade do item da nota com a unidade padrão do produto.
     * Retorna true se forem diferentes (ou seja, se deve forçar a conversão).
     */
    private boolean validaUnidade(BigDecimal codprod, String codvolNota) throws Exception {
        JapeWrapper proDAO = JapeFactory.dao(DynamicEntityNames.PRODUTO);
        DynamicVO proVO = proDAO.findByPK(codprod);

        String unidadePadrao = proVO.asString("CODVOL");

        return !unidadePadrao.equals(codvolNota);
    }
}