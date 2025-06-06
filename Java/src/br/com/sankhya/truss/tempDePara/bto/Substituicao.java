package br.com.sankhya.truss.tempDePara.bto;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import com.sankhya.util.JdbcUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;

public class Substituicao implements AcaoRotinaJava {
    @Override
    public void doAction(ContextoAcao ctx) throws Exception {
        Registro[] linhas = ctx.getLinhas();
        if (linhas.length != 1) {
            ctx.mostraErro("Selecione 1 registro.");
        }
        Registro linha = linhas[0];
        BigDecimal codtipoper = (BigDecimal) linha.getCampo("CODTIPOPER");
        if (codtipoper.compareTo(new BigDecimal("3187")) == 0) {
            ctx.setMensagemRetorno("Rotina não se aplica na TOP de Venda Outlet.");
            return;
        }
        BigDecimal codparc = (BigDecimal) linha.getCampo("CODPARC");
        if (!codParcFranqueado(codparc)) {
            ctx.setMensagemRetorno("Rotina só se aplica a parceiros franqueados.");
            return;
        }
        BigDecimal nunota = (BigDecimal) linha.getCampo("NUNOTA");
        Timestamp dtneg = (Timestamp) linha.getCampo("DTNEG");

        String localSep = getLocalSep(codparc);
        if (localSep == null) {
            ctx.mostraErro("Local de separação não informado no cadastro de parceiro.");
        }

        JapeWrapper daoItem = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
        Collection<DynamicVO> collectionItensNota = daoItem.find(" NUNOTA = ?", nunota);
        for (DynamicVO itemVO : collectionItensNota) {
            BigDecimal codprodDE = itemVO.asBigDecimal("CODPROD");
            BigDecimal qtdneg = itemVO.asBigDecimal("QTDNEG");
            BigDecimal codlocal = itemVO.asBigDecimal("CODLOCALORIG");
            BigDecimal multiploDE = getMultiplo(codprodDE);
            BigDecimal qtdAtendidaDE = getQtdAtendida(codprodDE, codlocal, multiploDE, localSep, qtdneg);


            if(qtdAtendidaDE.compareTo(BigDecimal.ZERO) == 0){
                BigDecimal codprodPARA = getCodProdPARA(codprodDE, dtneg);
                if(codprodPARA.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal multiploPARA = getMultiplo(codprodPARA);
                    BigDecimal qtdAtendidaPARA = getQtdAtendida(codprodPARA, codlocal, multiploPARA, localSep, qtdneg);
                    if(qtdAtendidaPARA.compareTo(qtdneg) == 0){
                        daoItem.prepareToUpdate(itemVO).set("CODPROD",codprodPARA).update();
                        //System.out.println(qtdneg + " do produto " + codprodDE + " substituido pelo produto " + codprodPARA);
                    } else if (qtdAtendidaPARA.compareTo(qtdneg) < 0 && qtdAtendidaPARA.compareTo(BigDecimal.ZERO) > 0) {
                        //incluirItem(itemVO,codprodPARA, qtdAtendidaPARA);
                        BigDecimal corte = qtdneg.subtract(qtdAtendidaPARA);
                        daoItem.prepareToUpdate(itemVO).set("QTDNEG",corte).update();
                    }
                }
            }

            if(qtdAtendidaDE.compareTo(BigDecimal.ZERO) > 0 && qtdAtendidaDE.compareTo(qtdneg) < 0){
                BigDecimal codprodPARA = getCodProdPARA(codprodDE, dtneg);
                if(codprodPARA.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal multiploPARA = getMultiplo(codprodPARA);
                    BigDecimal qtdParaRestante = qtdneg.subtract(qtdAtendidaDE);
                    BigDecimal qtdAtendidaPARA = getQtdAtendida(codprodPARA, codlocal, multiploPARA, localSep, qtdParaRestante);
                    if (qtdAtendidaPARA.compareTo(qtdParaRestante) <= 0) {
                        //incluirItem(itemVO,codprodPARA, qtdAtendidaPARA);
                        BigDecimal corte = qtdneg.subtract(qtdAtendidaPARA);
                        daoItem.prepareToUpdate(itemVO).set("QTDNEG",corte).update();
                    }
                }
            }


        }
    }

    private String getLocalSep(BigDecimal codparc) {
        try {
            DynamicVO parceiroVO = JapeFactory.dao(DynamicEntityNames.PARCEIRO).findByPK(codparc);
            return parceiroVO.asString("AD_LOCALSEPARACAO");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private BigDecimal getMultiplo(BigDecimal codprod) {
        BigDecimal multiplo = BigDecimal.ONE;
        try {
            DynamicVO produtoVO = JapeFactory.dao(DynamicEntityNames.PRODUTO).findByPK(codprod);
            BigDecimal qtdminvenda = produtoVO.asBigDecimal("AD_QTDMINVENDA");
            if (qtdminvenda != null) {
                multiplo = qtdminvenda;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return multiplo;
    }

    private BigDecimal getCodProdPARA(BigDecimal codprod, Timestamp dtneg) {
        BigDecimal para = BigDecimal.ZERO;
        JapeWrapper deParaProdDAO = JapeFactory.dao("AD_DEPARAPROD");
        try {
            Collection<DynamicVO> collection = deParaProdDAO.find(" DE = ? AND ? BETWEEN INIVIGENCIA AND FINVIGENCIA");
            if (!collection.isEmpty()) {
                DynamicVO deParaVO = collection.iterator().next();
                para = deParaVO.asBigDecimal("PARA");
                return para;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return para;
    }

    private boolean codParcFranqueado(BigDecimal codparc) {
        try {
            DynamicVO parceiroVO = JapeFactory.dao(DynamicEntityNames.PARCEIRO).findByPK(codparc);
            BigDecimal codtipparc = parceiroVO.asBigDecimal("CODTIPPARC");
            if (codtipparc.compareTo(new BigDecimal("10101001")) == 0) {
                return true;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    private BigDecimal getQtdAtendida(BigDecimal codprod, BigDecimal codlocal, BigDecimal multiplicador, String localSep, BigDecimal qtdneg) throws MGEModelException {
        JdbcWrapper jdbc = null;
        NativeSql sql = null;
        ResultSet rset = null;
        JapeSession.SessionHandle hnd = null;

        try {
            hnd = JapeSession.open();
            hnd.setFindersMaxRows(-1);
            EntityFacade entity = EntityFacadeFactory.getDWFFacade();
            jdbc = entity.getJdbcWrapper();
            jdbc.openSession();

            sql = new NativeSql(jdbc);

            if (localSep.equals("1")) {
                sql.appendSql("SELECT DISPONIVEL FROM AD_VW_ESTOQUEGLOBAL WHERE CODPROD = :CODPROD AND CODLOCAL = :CODLOCAL");
            } else {
                sql.appendSql("SELECT NVL(SUM(DISPONIVEL),0) AS DISPONIVEL FROM AD_VW_ESTOQUEPORPARCEIRO WHERE CODPROD = :CODPROD AND CODLOCAL = :CODLOCAL");
            }

            sql.setNamedParameter("CODPROD", codprod);
            sql.setNamedParameter("CODLOCAL", codlocal);

            rset = sql.executeQuery();

            if (rset.next()) {
                BigDecimal disponivel = rset.getBigDecimal("DISPONIVEL");
                if (disponivel.compareTo(qtdneg) >= 0) {
                    return qtdneg;
                } else if (disponivel.compareTo(multiplicador) < 0) {
                    return BigDecimal.ZERO;
                } else {
                    BigDecimal divisao = disponivel.divideToIntegralValue(multiplicador);
                    return divisao.multiply(multiplicador);
                }
            }
        } catch (Exception e) {
            MGEModelException.throwMe(e);
        } finally {
            JdbcUtils.closeResultSet(rset);
            NativeSql.releaseResources(sql);
            JdbcWrapper.closeSession(jdbc);
            JapeSession.close(hnd);
        }
        return BigDecimal.ZERO;
    }
}
