package br.com.sankhya.truss.corte.actions;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import com.sankhya.util.TimeUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;

public class CorteLocalOperador {
    private static JapeSession.SessionHandle hnd = null;
    private static JdbcWrapper jdbc	= null;
    private static Boolean isOpen = Boolean.FALSE;

    static BigDecimal nunota = null;
    static BigDecimal sequencia = null;
    static BigDecimal codprod = null;
    static BigDecimal qtdneg = null;
    public String executaCorteLocal(BigDecimal nunotaParam) throws Exception{
        // TODO Auto-generated method stub
        String statusPed = "20";
        Boolean deuErro = false;

        try {
            openSession();
            JapeWrapper proDAO = JapeFactory.dao(DynamicEntityNames.PRODUTO);
            JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
            JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
            String queryPedido = "";



            if(nunotaParam == null) {
                queryPedido = "SELECT NUNOTA "
                        + " FROM TGFCAB "
                        + " WHERE AD_STATUSPED = '2' "
                        + " AND AD_CORTOU IS NULL "
                        + " AND CODTIPOPER IN (SELECT CODTIPOPER FROM TGFTOP WHERE AD_CORTELOCAL = 'S')"
                        + " AND NVL(AD_BLOQCORTELOCAL,'N') = 'N'";
            } else {
                queryPedido = "SELECT NUNOTA FROM TGFCAB WHERE NUNOTA = :P_NUNOTA";
            }

            NativeSql queryPedidos = new NativeSql(jdbc);
            queryPedidos.setNamedParameter("P_NUNOTA", nunotaParam);
            ResultSet rsPedidos = queryPedidos.executeQuery(queryPedido);
            while(rsPedidos.next()) {

                BigDecimal nunota = rsPedidos.getBigDecimal("NUNOTA");
                this.nunota = nunota;

                NativeSql queryItens = new NativeSql(jdbc);
                queryItens.setNamedParameter("P_NUNOTA", nunota);
                ResultSet rsItens = queryItens.executeQuery("SELECT SEQUENCIA, CODPROD, CODEMP, CODLOCALORIG, QTDNEG FROM TGFITE WHERE NUNOTA = :P_NUNOTA");

                while(rsItens.next()) {
                    BigDecimal codprod = rsItens.getBigDecimal("CODPROD");
                    BigDecimal codemp = rsItens.getBigDecimal("CODEMP");
                    BigDecimal codlocalorig = rsItens.getBigDecimal("CODLOCALORIG");
                    BigDecimal qtdneg = rsItens.getBigDecimal("QTDNEG");
                    BigDecimal sequencia = rsItens.getBigDecimal("SEQUENCIA");
                    BigDecimal disponivel = BigDecimal.ZERO;
                    BigDecimal qtdMinVenda = null;


                    this.sequencia = sequencia;
                    this.codprod = codprod;
                    this.qtdneg = qtdneg;


                    NativeSql queryEstLocal = new NativeSql(jdbc);
                    queryEstLocal.setNamedParameter("P_CODPROD", codprod);
                    queryEstLocal.setNamedParameter("P_CODEMP", codemp);
                    queryEstLocal.setNamedParameter("P_CODLOCAL", codlocalorig);

                    try {


                        ResultSet rsEstLocal = queryEstLocal.executeQuery("SELECT SUM(DISPONIVEL) AS DISPONIVEL"
                                + " FROM AD_VW_ESTOQUELOCALOPERADOR "
                                + " WHERE CODPROD = :P_CODPROD "
                                + " AND CODEMP IN (:P_CODEMP, 0) "
                                + " AND CODLOCAL = :P_CODLOCAL ");

                        if(rsEstLocal.next()) {
                            disponivel = rsEstLocal.getBigDecimal("DISPONIVEL").add(qtdneg);
                        }

                    } catch (Exception e) {
                        deuErro = true;
                        if(nunotaParam == null) {
                            escreveLog("Erro ao realizar consulta de estoque local disponível: \n" + e.getMessage());
                        } else {
                            throw new Exception("Erro ao realizar consulta de estoque local disponível: \n" + e.getMessage());
                        }

                    }

                    if(disponivel == null) {
                        disponivel = BigDecimal.ZERO;
                    }


                    if(disponivel.compareTo(qtdneg) < 0) {
                        statusPed = "19";
                        DynamicVO proVO = proDAO.findByPK(codprod);
                        qtdMinVenda = proVO.asBigDecimal("AD_QTDMINVENDA");
                        if(qtdMinVenda == null) {
                            deuErro = true;
                            escreveLog("Produto " + codprod + " - " + proVO.asString("DESCRPROD") + " não possui cadastro de quantidade mínima para venda.\nRealize o cadastro.");
                        }

                        BigDecimal newQtdNeg = disponivel.subtract(disponivel.remainder(qtdMinVenda));

                        if(newQtdNeg.signum() <= 0) {
                            try {
                                iteDAO.prepareToUpdateByPK(nunota, sequencia)
                                        .set("QTDCONFERIDA", qtdneg)
                                        .update();
                            } catch (Exception e) {
                                deuErro = true;
                                if(nunotaParam == null) {
                                    escreveLog("Erro ao atualizar quantidade de corte do item. Quantidade de corte: " + qtdneg + "\n" + e.getMessage());
                                } else {
                                    throw new Exception("Erro ao atualizar quantidade de corte do item. Quantidade de corte: " + qtdneg + "\n" + e.getMessage());
                                }
                            }
                        } else {
                            try {
                                iteDAO.prepareToUpdateByPK(nunota, sequencia)
                                        .set("QTDCONFERIDA", qtdneg.subtract(newQtdNeg))
                                        .update();
                            } catch (Exception e) {
                                deuErro = true;
                                if(nunotaParam == null) {
                                    escreveLog("Erro ao atualizar quantidade de corte do item. Quantidade de corte: " + qtdneg.subtract(newQtdNeg) + "\n" + e.getMessage());
                                } else {
                                    throw new Exception("Erro ao atualizar quantidade de corte do item. Quantidade de corte: " + qtdneg.subtract(newQtdNeg) + "\n" + e.getMessage());
                                }
                            }
                        }

                    }
                }

                try {
                    if(!deuErro) {
                        cabDAO.prepareToUpdateByPK(nunota)
                                .set("AD_CORTOU", "S")
                                .set("AD_DHCORTELOCAL", TimeUtils.getNow())
                                .set("AD_STATUSPED", statusPed)
                                .update();
                    } else {
                        cabDAO.prepareToUpdateByPK(nunota)
                                .set("AD_CORTOU", null)
                                .set("AD_DHCORTELOCAL", null)
                                .set("AD_BLOQCORTELOCAL", "S")
                                .update();
                    }
                } catch (Exception e) {
                    if(nunotaParam == null) {
                        escreveLog("Erro ao atualizar dados do cabeçalho \n" + e.getMessage());
                    } else {
                        throw new Exception("Erro ao atualizar dados do cabeçalho \n" + e.getMessage());
                    }
                }
            }



        } catch(Exception e) {
            e.printStackTrace();
            escreveLog("Erro na ação agendada: \n" + e.getMessage());
        } finally {
            closeSession();
        }

        if(!deuErro) {
            return statusPed;
        } else {
            return "-1";
        }
    }

    private static void escreveLog(String msgError) throws Exception {
        try {
            JapeWrapper logDAO = JapeFactory.dao("AD_LOGCORTELOCAL");

            logDAO.create()
                    .set("NUNOTA", nunota)
                    .set("SEQUENCIAITE", sequencia)
                    .set("CODPROD", codprod)
                    .set("QTDNEG", qtdneg)
                    .set("MSGERRO", msgError)
                    .set("DHLOG", TimeUtils.getNow())
                    .save();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Erro ao escrever log: \n" + e.getMessage());
        }
    }

    private static void openSession() {
        try {
            if (isOpen && jdbc != null) {
                return;
            }

            EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
            hnd = JapeSession.open();
            hnd.setFindersMaxRows(-1);
            hnd.setCanTimeout(false);
            jdbc = dwfFacade.getJdbcWrapper();
            jdbc.openSession();
            isOpen = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void closeSession() {
        if(isOpen) {
            JdbcWrapper.closeSession(jdbc);
            JapeSession.close(hnd);
            isOpen = false;
            jdbc = null;
        }
    }
}
