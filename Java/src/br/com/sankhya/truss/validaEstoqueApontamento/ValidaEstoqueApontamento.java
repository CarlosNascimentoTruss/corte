package br.com.sankhya.truss.validaEstoqueApontamento;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

import java.math.BigDecimal;
import java.sql.ResultSet;


/*
   Evento na tabela TPRAMP com o objetivo de não permitir realizar o apontamento de materiais com lote inativo e nem com estoque insuficiente.
   Atendimento ao briefing: https://grupoboticario.kanbanize.com/ctrl_board/301/cards/1705816/details/
 */

public class ValidaEstoqueApontamento implements EventoProgramavelJava {


    @Override
    public void beforeInsert(PersistenceEvent event) throws Exception {
        validaEstoque(event);
    }

    @Override
    public void beforeUpdate(PersistenceEvent event) throws Exception {
        validaEstoque(event);
    }

    @Override
    public void beforeDelete(PersistenceEvent event) throws Exception {

    }

    @Override
    public void afterInsert(PersistenceEvent event) throws Exception {

    }

    @Override
    public void afterUpdate(PersistenceEvent event) throws Exception {

    }

    @Override
    public void afterDelete(PersistenceEvent event) throws Exception {

    }

    @Override
    public void beforeCommit(TransactionContext tranCtx) throws Exception {

    }


    private static void validaEstoque(PersistenceEvent evt) throws Exception {
        EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
        JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
        try {
            DynamicVO newVO = (DynamicVO) evt.getVo();

            String controle = newVO.asString("CONTROLEMP");
            BigDecimal codprod = newVO.asBigDecimal("CODPRODMP");
            BigDecimal qtdApontada = newVO.asBigDecimal("QTD");
            BigDecimal codlocal = newVO.asBigDecimal("CODLOCALBAIXA");

            // Validação apenas para itens com preenchimento de lote
            if (controle.equals(" ") || controle == null) {
                return;
            }

            // Query identificando o lote ativo ou inativo
            NativeSql query = new NativeSql(jdbc);
            query.setNamedParameter("P_CODPROD", codprod);
            query.setNamedParameter("P_CONTROLE", controle);
            ResultSet r = query.executeQuery("SELECT DISTINCT NVL(AD_ATIVOLOTE,'N') AS ATIVOLOTE FROM TGFEST WHERE CODPROD = :P_CODPROD AND CONTROLE = :P_CONTROLE");

            String ativoLote = "N";
            if(r.next()) {
                ativoLote = r.getString("ATIVOLOTE");
            }

            //  Se o lote for inativo, lança o erro.
            if(ativoLote.equals("N")) {
                throw new Exception("Lote do produto " + codprod + " e controle " + controle + " encontra-se inativo.");
            }


            /*
                Query obtendo a quantidade disponível em estoque para o produto.
                É considerado aqui o local de baixa na própria TPRAMP, empresa 6, lote ativo, produto e lote sendo apontados.

             */
            NativeSql query2 = new NativeSql(jdbc);
            query2.setNamedParameter("P_CODPROD", codprod);
            query2.setNamedParameter("P_CONTROLE", controle);
            query2.setNamedParameter("P_CODLOCAL", codlocal);
            ResultSet r2 = query2.executeQuery("SELECT SUM(ESTOQUE - RESERVADO) AS ESTOQUEDISPONIVEL" +
                    " FROM TGFEST " +
                    " WHERE CODPROD = :P_CODPROD" +
                    " AND CONTROLE = :P_CONTROLE" +
                    " AND CODEMP = 6 " +
                    " AND CODLOCAL = :P_CODLOCAL " +
                    " AND AD_ATIVOLOTE = 'S' ");

            BigDecimal estoqueDisponivel = BigDecimal.ZERO;

            if(r2.next()){
                estoqueDisponivel = r2.getBigDecimal("ESTOQUEDISPONIVEL");
            }

            /*
              Caso estoque for indisponível, lança mensagem de erro.
             */
            if(qtdApontada.compareTo(estoqueDisponivel)> 0){
                throw new Exception("Não foi possível seguir com o apontamento. Quantidade apontada ("
                                     + qtdApontada + ") é maior que a quantidade disponível ("
                                     + estoqueDisponivel + ") para o produto "
                                      + codprod + " controle " + controle);
            }


        } catch(Exception e) {
            e.printStackTrace();
            throw new Exception("Erro no evento Valida Estoque Apontamento: " + e.getMessage());
        } finally {
            jdbc.closeSession();
        }





    }


}
