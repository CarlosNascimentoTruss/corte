package br.com.sankhya.truss.corte.actions;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.CentralItemNota;
import br.com.sankhya.modelcore.comercial.centrais.CACHelper;
import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.truss.corte.helper.CorteHelper;
import br.com.sankhya.ws.ServiceContext;
import com.sankhya.util.TimeUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CorteExpedicaoOperador {

    public static void executaCorte(BigDecimal nunota) throws Exception {
        try {
            JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
            JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
            JapeWrapper proDAO = JapeFactory.dao(DynamicEntityNames.PRODUTO);
            EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
            JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();

            ServiceContext sctx = new ServiceContext(null);
            sctx.setAutentication(AuthenticationInfo.getCurrent());
            sctx.makeCurrent();

            DynamicVO cabVO = cabDAO.findByPK(nunota);

            if (!cabVO.asString("AD_STATUSPED").equals("4")) {
                throw new Exception("O Pedido deve estar com status Pedido Aprovado.");
            }

            Collection<DynamicVO> itesVO = iteDAO.find("NUNOTA = ?", nunota);
            int qtdItens = itesVO.size();
            int countDel = 0;

            for (DynamicVO iteVO : itesVO) {
                BigDecimal disponivel = BigDecimal.ZERO;
                BigDecimal codprod = iteVO.asBigDecimal("CODPROD");
                BigDecimal codlocal = iteVO.asBigDecimal("CODLOCALORIG");
                BigDecimal qtdneg = iteVO.asBigDecimal("QTDNEG");
                BigDecimal qtdMinVenda = BigDecimal.ZERO;
                BigDecimal sequencia = iteVO.asBigDecimal("SEQUENCIA");
                NativeSql query = new NativeSql(jdbc);
                query.setNamedParameter("P_CODPROD", codprod);
                query.setNamedParameter("P_CODLOCAL", codlocal);


                ResultSet r = query.executeQuery("SELECT NVL(SUM(DISPONIVEL),0) AS DISPONIVEL FROM AD_VW_ESTOQUEPORPARCEIRO WHERE CODPROD = :P_CODPROD AND CODLOCAL = :P_CODLOCAL");

                if (r.next()) {
                    disponivel = r.getBigDecimal("DISPONIVEL");
                }

                if (disponivel.compareTo(qtdneg) < 0) {

                    DynamicVO proVO = proDAO.findByPK(codprod);
                    qtdMinVenda = proVO.asBigDecimal("AD_QTDMINVENDA");
                    if (qtdMinVenda == null) {
                        throw new Exception("Produto " + codprod + " - " + proVO.asString("DESCRPROD") + " npossui cadastro de quantidade mpara venda.\nRealize o cadastro.");
                    }

                    BigDecimal newQtdNeg = disponivel.subtract(disponivel.remainder(qtdMinVenda));

                    iteDAO.prepareToUpdateByPK(nunota, sequencia)
                            .set("AD_CLASSCORT", "RP")
                            .update();


                    if (newQtdNeg.signum() <= 0) {
                        try {
                            countDel++;
                            iteDAO.prepareToUpdateByPK(nunota, sequencia)
                                    .set("AD_CLASSCORT", "RT")
                                    .update();

                            iteDAO.delete(new Object[]{nunota, sequencia});
                        } catch (Exception e) {
                            throw new Exception("Erro ao deletar\n" + e.getMessage());
                        }
                    } else {

                        iteVO.setProperty("QTDNEG", newQtdNeg);
                        iteVO.setProperty("VLRTOT", newQtdNeg.multiply(iteVO.asBigDecimal("VLRUNIT")));

                        CentralItemNota itemNota = new CentralItemNota();
                        itemNota.recalcularValores("QTDNEG", newQtdNeg.toString(), iteVO, nunota);


                        List<DynamicVO> itensVO = new ArrayList<DynamicVO>();
                        itensVO.add(iteVO);

                        CACHelper cacHelper = new CACHelper();
                        cacHelper.incluirAlterarItem(nunota, sctx, null, true, itensVO);

                        iteVO.setProperty("AD_CLASSCORT", null);
                        dwfEntityFacade.saveEntity(DynamicEntityNames.ITEM_NOTA, (EntityVO) iteVO);
                    }
                }


            }

            // Atualiza a TOP de Corte
            BigDecimal topCorte = CorteHelper.buscaTopCorte(cabVO, jdbc);
            Timestamp dhTopCorte = null;
            NativeSql sql = new NativeSql(jdbc);
            sql.setNamedParameter("P_CODTIPOPER", topCorte);
            ResultSet rs = sql.executeQuery("SELECT MAX(DHALTER) AS DHALTER FROM TGFTOP WHERE CODTIPOPER = :P_CODTIPOPER");
            if (rs.next()) {
                dhTopCorte = rs.getTimestamp("DHALTER");
            }


            cabDAO.prepareToUpdate(cabVO)
                    .set("CODTIPOPER", topCorte)
                    .set("DHTIPOPER", dhTopCorte)
                    .update();


            // Atualiza itens para reservar
            NativeSql q = new NativeSql(jdbc);
            q.setNamedParameter("P_NUNOTA", nunota);
            ResultSet r = q.executeQuery("SELECT NUNOTA, SEQUENCIA FROM TGFITE WHERE NUNOTA = :P_NUNOTA");

            while (r.next()) {
                iteDAO.prepareToUpdateByPK(r.getBigDecimal("NUNOTA"), r.getBigDecimal("SEQUENCIA"))
                        .set("ATUALESTOQUE", BigDecimal.ONE)
                        .set("RESERVA", "S")
                        .update();
            }


            if (countDel == qtdItens) {
                cabDAO.prepareToUpdateByPK(nunota)
                        .set("AD_CLASSCORTE", "RT")
                        .update();

                cabDAO.deleteByCriteria("NUNOTA = ?", nunota);
            } else {
                indicaLotes(nunota);
                cabDAO.prepareToUpdate(cabVO)
                        .set("AD_DTLIBEXP", TimeUtils.getNow())
                        .set("AD_STATUSPED", "28")
                        .update();

            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
    }



    private static void indicaLotes(BigDecimal nunota) throws Exception {
        JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
        JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
        Collection<DynamicVO> itesVO = iteDAO.find("NUNOTA = ?", nunota);
        EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
        JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();



        try {

            cabDAO.prepareToUpdateByPK(nunota)
                    .set("AD_DESCONSCORTE", "S")
                    .update();
            for (DynamicVO iteVO : itesVO) {
                NativeSql query = new NativeSql(jdbc);
                query.setNamedParameter("P_CODPROD", iteVO.asBigDecimal("CODPROD"));
                query.setNamedParameter("P_CODEMP", iteVO.asBigDecimal("CODEMP"));
                query.setNamedParameter("P_CODLOCAL", iteVO.asBigDecimal("CODLOCALORIG"));
                ResultSet r = query.executeQuery("SELECT CODPROD, CONTROLE, NVL(DISPONIVEL,0) AS DISPONIVEL " +
                        " FROM AD_VW_ESTOQUEPORPARCEIRO EST " +
                        " WHERE CODPROD = :P_CODPROD " +
                        " AND EST.CODLOCAL = :P_CODLOCAL " +
                        " AND DISPONIVEL > 0 " +
                        " ORDER BY DTVAL ");
                BigDecimal qtdRestante = iteVO.asBigDecimal("QTDNEG");
                int count = 0;

                while(r.next()) {
                    BigDecimal disponivel = r.getBigDecimal("DISPONIVEL");
                    String controle = r.getString("CONTROLE");

                    if (count == 0) {
                        if (disponivel.compareTo(qtdRestante) >= 0) {
                            iteVO.setProperty("CONTROLE", controle);
                            dwfEntityFacade.saveEntity(DynamicEntityNames.ITEM_NOTA, (EntityVO) iteVO);
                            break;
                        } else {
                            iteVO.setProperty("QTDNEG", disponivel);
                            iteVO.setProperty("VLRTOT", disponivel.multiply(iteVO.asBigDecimal("VLRUNIT")));
                            iteVO.setProperty("CONTROLE", controle);
                            dwfEntityFacade.saveEntity(DynamicEntityNames.ITEM_NOTA, (EntityVO) iteVO);
                            qtdRestante = qtdRestante.subtract(disponivel);
                        }
                    } else {
                        if (disponivel.compareTo(qtdRestante) >= 0) {
                            insereItem(iteVO, qtdRestante, controle);
                            break;
                        } else {
                            insereItem(iteVO, disponivel, controle);
                            qtdRestante = qtdRestante.subtract(disponivel);
                        }
                    }

                    count++;
                }

                cabDAO.prepareToUpdateByPK(nunota)
                        .set("AD_DESCONSCORTE", "N")
                        .update();
            }
            recalculaNota(nunota);
        } catch(Exception e){
            e.printStackTrace();
            throw new Exception("Erro ao indicar Lotes: " + e.getMessage());
        } finally {
            jdbc.closeSession();
        }



    }

    private static void insereItem (DynamicVO iteVO, BigDecimal quantidade, String controle) throws Exception {

        JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
        try {

            iteDAO.create()
                    .set("NUNOTA", iteVO.asBigDecimal("NUNOTA"))
                    .set("CODPROD", iteVO.asBigDecimal("CODPROD"))
                    .set("QTDNEG", quantidade)
                    .set("CONTROLE", controle)
                    .set("VLRUNIT", iteVO.asBigDecimal("VLRUNIT"))
                    .set("VLRTOT", iteVO.asBigDecimal("VLRUNIT").multiply(quantidade))
                    .set("CODLOCALORIG", iteVO.asBigDecimal("CODLOCALORIG"))
                    .set("CODVOL", iteVO.asString("CODVOL"))
                    .set("ATUALESTOQUE", iteVO.asBigDecimal("ATUALESTOQUE"))
                    .set("RESERVA", iteVO.asString("RESERVA"))
                    .set("NUTAB", iteVO.asBigDecimal("NUTAB"))
                    .save();
        } catch(Exception e) {
            throw new Exception("Erro ao incluir itens de lote: " + e.getMessage());
        }



    }


    public static void recalculaNota(BigDecimal nunota) throws Exception {
        JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
        DynamicVO cabVO = cabDAO.findByPK(nunota);

        EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
        PersistentLocalEntity persistentEntityCab = dwfEntityFacade.findEntityByPrimaryKey(DynamicEntityNames.CABECALHO_NOTA, new Object[] { nunota});

        ImpostosHelpper imposto = new ImpostosHelpper();
        imposto.carregarNota(nunota);
        imposto.calculaICMS(true);

        imposto.totalizarNota(nunota);
        imposto.setForcarRecalculo(true);
        imposto.setAtualizaImpostos(true);
        imposto.setCalcularTudo(true);
        imposto.calcularImpostos(nunota);
        imposto.salvarNota();

        BigDecimal totalNota = imposto.calcularTotalNota(cabVO.asBigDecimal("NUNOTA"), imposto.calcularTotalItens(cabVO.asBigDecimal("NUNOTA"), false));

        cabVO.setProperty("VLRNOTA", totalNota);
        persistentEntityCab.setValueObject((EntityVO)cabVO);
    }


}
