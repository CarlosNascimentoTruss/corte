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
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.truss.util.Duplicate;
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
        Timestamp dtEntSai = (Timestamp) linha.getCampo("DTENTSAI");
        BigDecimal codUsuLogado = ctx.getUsuarioLogado();

        String localSep = getLocalSep(codparc);
        if (localSep == null) {
            ctx.mostraErro("Local de separação não informado no cadastro de parceiro.");
        }

        JapeWrapper daoItem = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
        Collection<DynamicVO> collectionItensNota = daoItem.find(" NUNOTA = ?", nunota);
        for (DynamicVO itemVO : collectionItensNota) {
            BigDecimal sequencia = itemVO.asBigDecimal("SEQUENCIA");
            BigDecimal codprodDE = itemVO.asBigDecimal("CODPROD");
            BigDecimal qtdneg = itemVO.asBigDecimal("QTDNEG");
            BigDecimal codlocal = itemVO.asBigDecimal("CODLOCALORIG");
            BigDecimal multiploDE = getMultiplo(codprodDE);
            BigDecimal qtdAtendidaDE = getQtdAtendida(codprodDE, codlocal, multiploDE, localSep, qtdneg);
            if (qtdAtendidaDE.compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal codprodPARA = getCodProdPARA(codprodDE, dtEntSai);
                if (codprodPARA.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal multiploPARA = getMultiplo(codprodPARA);
                    BigDecimal qtdAtendidaPARA = getQtdAtendida(codprodPARA, codlocal, multiploPARA, localSep, qtdneg);
                    if (qtdAtendidaPARA.compareTo(qtdneg) == 0) {
                        daoItem.prepareToUpdate(itemVO).set("CODPROD", codprodPARA).update();
                        atualizaTotais(nunota, sequencia);
                        registraAlteracaoLOG(codprodDE,codUsuLogado,dtEntSai,nunota,"TOTAL: " + qtdneg + " do produto " + codprodDE + " substituido pelo produto " + codprodPARA);
                    } else if (qtdAtendidaPARA.compareTo(qtdneg) < 0 && qtdAtendidaPARA.compareTo(BigDecimal.ZERO) > 0) {
                        duplicarItem(itemVO, codprodPARA, qtdAtendidaPARA);
                        BigDecimal corte = qtdneg.subtract(qtdAtendidaPARA);
                        daoItem.prepareToUpdate(itemVO).set("QTDNEG", corte).update();
                        atualizaTotais(nunota, sequencia);
                        registraAlteracaoLOG(codprodDE,codUsuLogado,dtEntSai,nunota,"PARCIAL: " + qtdAtendidaPARA + " do produto " + codprodDE + " substituido pelo produto " + codprodPARA);
                    }
                }
            }

            if (qtdAtendidaDE.compareTo(BigDecimal.ZERO) > 0 && qtdAtendidaDE.compareTo(qtdneg) < 0) {
                BigDecimal codprodPARA = getCodProdPARA(codprodDE, dtEntSai);
                if (codprodPARA.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal multiploPARA = getMultiplo(codprodPARA);
                    BigDecimal qtdParaRestante = qtdneg.subtract(qtdAtendidaDE);
                    BigDecimal qtdAtendidaPARA = getQtdAtendida(codprodPARA, codlocal, multiploPARA, localSep, qtdParaRestante);
                    if (qtdAtendidaPARA.compareTo(qtdParaRestante) <= 0) {
                        duplicarItem(itemVO, codprodPARA, qtdAtendidaPARA);
                        BigDecimal corte = qtdneg.subtract(qtdAtendidaPARA);
                        daoItem.prepareToUpdate(itemVO).set("QTDNEG", corte).update();
                        registraAlteracaoLOG(codprodDE,codUsuLogado,dtEntSai,nunota,"PARCIAL: " + qtdAtendidaPARA + " do produto " + codprodDE + " substituido pelo produto " + codprodPARA);
                    }
                }
            }
        }
        ctx.setMensagemRetorno("DePara Executado com sucesso!");
    }

    private void registraAlteracaoLOG(BigDecimal codprodDE, BigDecimal codUsuLogado, Timestamp dtEntSai, BigDecimal nunota, String msg) throws Exception {
        JapeWrapper daoDeParaExec = JapeFactory.dao("AD_DEPARAPRODEXEC");
        FluidCreateVO fluid = daoDeParaExec.create();
        fluid.set("SEQ",getSeqExec(daoDeParaExec));
        fluid.set("NUNOTA",nunota);
        fluid.set("CODPRODORIGINAL",codprodDE);
        fluid.set("CODUSU",codUsuLogado);
        fluid.set("DHEXEC",new Timestamp(System.currentTimeMillis()));
        fluid.set("DHENTSAI",dtEntSai);
        fluid.set("REGRAAPLICADA",msg);
        fluid.save();
    }

    private BigDecimal getSeqExec(JapeWrapper daoDeParaExec) {
        BigDecimal nextSeq = BigDecimal.ONE;
        try {
            Collection<DynamicVO> collection = daoDeParaExec.find(" SEQ = (SELECT MAX(SEQ) FROM AD_DEPARAPRODEXEC)");
            if(collection.isEmpty()){
                return nextSeq;
            }else{
                DynamicVO vo = collection.iterator().next();
                BigDecimal lastSeq = vo.asBigDecimal("SEQ");
                return lastSeq.add(BigDecimal.ONE);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void duplicarItem(DynamicVO itemVO, BigDecimal codprodPARA, BigDecimal qtdAtendidaPARA) {
        BigDecimal nunota = itemVO.asBigDecimal("NUNOTA");
        JapeWrapper daoItem = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
        try {
            FluidCreateVO newItem = Duplicate.duplicate(daoItem, itemVO);
            BigDecimal nextSeqItem = getSeqItem(nunota);
            newItem.set("SEQUENCIA", nextSeqItem);
            newItem.set("CODPROD", codprodPARA);
            newItem.set("QTDNEG", qtdAtendidaPARA);
            newItem.save();

            atualizaTotais(nunota, nextSeqItem);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void atualizaTotais(BigDecimal nunota, BigDecimal nextSeqItem) {
        JapeWrapper dao = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
        try {
            DynamicVO vo = dao.findByPK(nunota, nextSeqItem);
            BigDecimal qtdneg = vo.asBigDecimal("QTDNEG");
            BigDecimal vlrunit = vo.asBigDecimal("VLRUNIT");
            FluidUpdateVO update = dao.prepareToUpdateByPK(nunota, nextSeqItem);
            BigDecimal total = qtdneg.multiply(vlrunit);
            update.set("VLRTOT",total);
            update.set("BASEICMS", total);
            update.set("BASEIPI", total);
            update.update();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private BigDecimal getSeqItem(BigDecimal nunota) {
        BigDecimal seq = BigDecimal.ONE;
        try {
            JapeWrapper daoItem = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
            Collection<DynamicVO> collection = daoItem.find(" NUNOTA = ? AND SEQUENCIA = (SELECT MAX(SEQUENCIA) FROM TGFITE WHERE NUNOTA = ?)", nunota, nunota);
            if(!collection.isEmpty()){
                DynamicVO itemVO = collection.iterator().next();
                BigDecimal sequenciaMax = itemVO.asBigDecimal("SEQUENCIA");
                return sequenciaMax.add(BigDecimal.ONE);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return seq;
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

    private BigDecimal getCodProdPARA(BigDecimal codprod, Timestamp dtEntSai) {
        BigDecimal para = BigDecimal.ZERO;
        JapeWrapper deParaProdDAO = JapeFactory.dao("AD_DEPARAPROD");
        try {
            Collection<DynamicVO> collection = deParaProdDAO.find(" DE = ? AND ? BETWEEN INIVIGENCIA AND FINVIGENCIA", codprod, dtEntSai);
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
