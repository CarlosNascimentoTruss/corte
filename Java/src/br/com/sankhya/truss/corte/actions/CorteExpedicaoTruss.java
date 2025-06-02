package br.com.sankhya.truss.corte.actions;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.CentralItemNota;
import br.com.sankhya.modelcore.comercial.centrais.CACHelper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.truss.corte.helper.CorteHelper;
import br.com.sankhya.truss.corte.scheduled.CorteLocal;
import br.com.sankhya.ws.ServiceContext;
import com.sankhya.util.TimeUtils;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CorteExpedicaoTruss {
    public static void executaCorte(BigDecimal nunota) throws Exception {
        try {
            JapeWrapper iteDAO = JapeFactory.dao("ItemNota");
            JapeWrapper cabDAO = JapeFactory.dao("CabecalhoNota");
            JapeWrapper proDAO = JapeFactory.dao("Produto");
            JapeWrapper parDAO = JapeFactory.dao("Parceiro");
            EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
            JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
            ServiceContext sctx = new ServiceContext(null);
            sctx.setAutentication(AuthenticationInfo.getCurrent());
            sctx.makeCurrent();
            DynamicVO cabVO = cabDAO.findByPK(new Object[] { nunota });
            if (!cabVO.asString("AD_STATUSPED").equals("4"))
                throw new Exception("O Pedido deve estar com status Pedido Aprovado.");
            Collection<DynamicVO> itesVO = iteDAO.find("NUNOTA = ?", new Object[] { nunota });
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
                ResultSet resultSet = query.executeQuery("SELECT DISPONIVEL FROM AD_VW_ESTOQUEGLOBAL WHERE CODPROD = :P_CODPROD AND CODLOCAL = :P_CODLOCAL");
                if (resultSet.next())
                    disponivel = resultSet.getBigDecimal("DISPONIVEL");
                if (disponivel.compareTo(qtdneg) < 0) {
                    DynamicVO proVO = proDAO.findByPK(new Object[] { codprod });
                    qtdMinVenda = proVO.asBigDecimal("AD_QTDMINVENDA");
                    if (qtdMinVenda == null)
                        throw new Exception("Produto " + codprod + " - " + proVO.asString("DESCRPROD") + " npossui cadastro de quantidade mpara venda.\nRealize o cadastro.");
                    BigDecimal newQtdNeg = disponivel.subtract(disponivel.remainder(qtdMinVenda));
                    ((FluidUpdateVO)iteDAO.prepareToUpdateByPK(new Object[] { nunota, sequencia }).set("AD_CLASSCORT", "RP"))
                            .update();
                    if (newQtdNeg.signum() <= 0) {
                        try {
                            countDel++;
                            ((FluidUpdateVO)iteDAO.prepareToUpdateByPK(new Object[] { nunota, sequencia }).set("AD_CLASSCORT", "RT"))
                                    .update();
                            iteDAO.delete(new Object[] { nunota, sequencia });
                        } catch (Exception e) {
                            throw new Exception("Erro ao deletar\n" + e.getMessage());
                        }
                        continue;
                    }
                    iteVO.setProperty("QTDNEG", newQtdNeg);
                    iteVO.setProperty("VLRTOT", newQtdNeg.multiply(iteVO.asBigDecimal("VLRUNIT")));
                    CentralItemNota itemNota = new CentralItemNota();
                    itemNota.recalcularValores("QTDNEG", newQtdNeg.toString(), iteVO, nunota);
                    List<DynamicVO> itensVO = new ArrayList<>();
                    itensVO.add(iteVO);
                    CACHelper cacHelper = new CACHelper();
                    cacHelper.incluirAlterarItem(nunota, sctx, null, true, itensVO);
                    iteVO.setProperty("AD_CLASSCORT", null);
                    dwfEntityFacade.saveEntity("ItemNota", (EntityVO)iteVO);
                }
            }
            BigDecimal topCorte = CorteHelper.buscaTopCorte(cabVO, jdbc);
            Timestamp dhTopCorte = null;
            NativeSql sql = new NativeSql(jdbc);
            sql.setNamedParameter("P_CODTIPOPER", topCorte);
            ResultSet rs = sql.executeQuery("SELECT MAX(DHALTER) AS DHALTER FROM TGFTOP WHERE CODTIPOPER = :P_CODTIPOPER");
            if (rs.next())
                dhTopCorte = rs.getTimestamp("DHALTER");
            ((FluidUpdateVO)((FluidUpdateVO)cabDAO.prepareToUpdate(cabVO)
                    .set("CODTIPOPER", topCorte))
                    .set("DHTIPOPER", dhTopCorte))
                    .update();
            NativeSql q = new NativeSql(jdbc);
            q.setNamedParameter("P_NUNOTA", nunota);
            ResultSet r = q.executeQuery("SELECT NUNOTA, SEQUENCIA FROM TGFITE WHERE NUNOTA = :P_NUNOTA");
            while (r.next()) {
                ((FluidUpdateVO)((FluidUpdateVO)iteDAO.prepareToUpdateByPK(new Object[] { r.getBigDecimal("NUNOTA"), r.getBigDecimal("SEQUENCIA") }).set("ATUALESTOQUE", BigDecimal.ONE))
                        .set("RESERVA", "S"))
                        .update();
            }
            if (countDel == qtdItens) {
                ((FluidUpdateVO)cabDAO.prepareToUpdateByPK(new Object[] { nunota }).set("AD_CLASSCORTE", "RT"))
                        .update();
                cabDAO.deleteByCriteria("NUNOTA = ?", new Object[] { nunota });
            } else {
                CorteLocal corte = new CorteLocal();
                String statusPed = corte.executaCorteLocal(nunota);
                if (statusPed.equals("-1"))
                    throw new Exception("Erro ao executar Corte Local");
                ((FluidUpdateVO)cabDAO.prepareToUpdate(cabVO)
                        .set("AD_STATUSPED", statusPed))
                        .update();
            }
            ((FluidUpdateVO)cabDAO.prepareToUpdate(cabVO)
                    .set("AD_DTLIBEXP", TimeUtils.getNow()))
                    .update();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
    }
}
