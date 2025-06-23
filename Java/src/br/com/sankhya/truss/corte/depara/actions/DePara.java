package br.com.sankhya.truss.corte.depara.actions;

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
import br.com.sankhya.truss.corte.depara.util.Duplicate;
import com.sankhya.util.JdbcUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;

public class DePara {

    public void acao(BigDecimal nunota, BigDecimal codUsuLogado) throws Exception {
        JapeWrapper daoCAB = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
        DynamicVO cabVO = daoCAB.findByPK(nunota);
        daoCAB.prepareToUpdateByPK(nunota).set("AD_DESCONSCORTE", "S").update();

        BigDecimal codtipoper = cabVO.asBigDecimal("CODTIPOPER");
        if (codtipoper.compareTo(new BigDecimal("3187")) == 0) {
            return;
        }
        BigDecimal codparc = cabVO.asBigDecimal("CODPARC");
        if (!parcFranqueado(codparc)) {
            return;
        }
        Timestamp dtEntSai = cabVO.asTimestamp("DTENTSAI");
        BigDecimal codEmp = cabVO.asBigDecimal("CODEMP");

        String localSep = getLocalSep(codparc);

        JapeWrapper daoItem = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
        Collection<DynamicVO> collectionItensNota = daoItem.find(" NUNOTA = ?", nunota);
        for (DynamicVO itemVO : collectionItensNota) {
            BigDecimal sequencia = itemVO.asBigDecimal("SEQUENCIA");
            BigDecimal codProdOriginal = itemVO.asBigDecimal("CODPROD");
            BigDecimal qtdNegOriginal = itemVO.asBigDecimal("QTDNEG");
            BigDecimal qtdParaContemplar = qtdNegOriginal;
            BigDecimal codlocal = itemVO.asBigDecimal("CODLOCALORIG");

            ArrayList<DynamicVO> regrasSuperiores = getRegras(codProdOriginal,dtEntSai,"PARA",codEmp,codlocal);
            for (int i = 0; i < regrasSuperiores.size() && qtdParaContemplar.compareTo(BigDecimal.ZERO) > 0; i++){
                DynamicVO regraSuperior = regrasSuperiores.get(i);
                BigDecimal codProdDE = regraSuperior.asBigDecimal("DE");
                BigDecimal qtdAtendida = getQtdAtendida(codProdDE,codlocal,localSep,qtdParaContemplar);
                if(qtdAtendida.compareTo(BigDecimal.ZERO) > 0){
                    StringBuilder mensagemRegra = new StringBuilder();
                    mensagemRegra.append("Regra Superior: ");
                    if(qtdAtendida.compareTo(qtdParaContemplar) == 0){
                        daoItem.prepareToUpdate(itemVO).set("CODPROD", codProdDE).set("QTDNEG",qtdAtendida).update();
                        atualizaTotais(nunota, sequencia);
                    }else{
                        duplicarItem(itemVO,codProdDE,qtdAtendida);
                    }
                    mensagemRegra.append(qtdAtendida).append(" do produto ").append(codProdOriginal).append(" substituido pelo produto ").append(codProdDE);
                    registraAlteracaoLOG(codProdDE,codUsuLogado,dtEntSai,nunota,mensagemRegra.toString());

                    qtdParaContemplar = qtdParaContemplar.subtract(qtdAtendida);
                }
            }

            BigDecimal qtdAtendidaProdOriginal = getQtdAtendida(codProdOriginal, codlocal, localSep, qtdParaContemplar);
            if(qtdAtendidaProdOriginal.compareTo(BigDecimal.ZERO) > 0 && qtdParaContemplar.compareTo(BigDecimal.ZERO) == 0){
                if (qtdNegOriginal.compareTo(qtdAtendidaProdOriginal) == 0) {
                    return;
                }
                if(qtdAtendidaProdOriginal.compareTo(qtdParaContemplar) == 0) {
                     daoItem.prepareToUpdate(itemVO).set("QTDNEG", qtdAtendidaProdOriginal).update();
                     atualizaTotais(nunota, sequencia);
                }else{
                    duplicarItem(itemVO,codProdOriginal,qtdAtendidaProdOriginal);
                    qtdParaContemplar = qtdParaContemplar.subtract(qtdAtendidaProdOriginal);
                }
            }

            ArrayList<DynamicVO> regrasInferiores = getRegras(codProdOriginal,dtEntSai,"DE",codEmp,codlocal);
            for (int i = 0; i < regrasInferiores.size() && qtdParaContemplar.compareTo(BigDecimal.ZERO) > 0; i++){
                DynamicVO regraInferior = regrasInferiores.get(i);
                BigDecimal codProdPARA = regraInferior.asBigDecimal("PARA");
                BigDecimal qtdAtendida = getQtdAtendida(codProdPARA,codlocal,localSep,qtdParaContemplar);
                if(qtdAtendida.compareTo(BigDecimal.ZERO) > 0){
                    StringBuilder mensagemRegra = new StringBuilder();
                    mensagemRegra.append("Regra Inferior: ");
                    if(qtdAtendida.compareTo(qtdParaContemplar) == 0){
                        daoItem.prepareToUpdate(itemVO).set("CODPROD", codProdPARA).set("QTDNEG",qtdAtendida).update();
                        atualizaTotais(nunota, sequencia);
                    }else{
                        duplicarItem(itemVO,codProdPARA,qtdAtendida);
                    }
                    mensagemRegra.append(qtdAtendida).append(" do produto ").append(codProdOriginal).append(" substituido pelo produto ").append(codProdPARA);
                    registraAlteracaoLOG(codProdPARA,codUsuLogado,dtEntSai,nunota,mensagemRegra.toString());

                    qtdParaContemplar = qtdParaContemplar.subtract(qtdAtendida);
                }
            }

            if(qtdAtendidaProdOriginal.compareTo(BigDecimal.ZERO) > 0){
                BigDecimal qtdnegProdOriginal = daoItem.findByPK(new Object[]{nunota, sequencia}).asBigDecimal("QTDNEG");
                qtdnegProdOriginal = qtdnegProdOriginal.add(qtdAtendidaProdOriginal);
                daoItem.prepareToUpdate(itemVO).set("QTDNEG", qtdnegProdOriginal).update();
                atualizaTotais(nunota, sequencia);
            }
        }
        daoCAB.prepareToUpdateByPK(nunota).set("AD_DESCONSCORTE", null).update();
    }

    private ArrayList<DynamicVO> getRegras(BigDecimal codProd, Timestamp dtEntSai, String busca, BigDecimal codEmp, BigDecimal codLocal) {
        String contrapartida = busca.equals("DE")?"PARA":"DE";
        ArrayList<DynamicVO> listDeParaVO = new ArrayList<>();
        JapeWrapper daoDePara = JapeFactory.dao("AD_DEPARAPROD");
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
            sql.appendSql("SELECT X.SEQ FROM ( ");
            sql.appendSql("        SELECT ");
            sql.appendSql("        DP.*, ");
            sql.appendSql("        (SELECT MIN(DTVAL) FROM TGFEST E WHERE E.CODPROD = DP." + busca + " AND E.CODEMP = :CODEMP AND E.CODLOCAL = :CODLOCAL) AS MINVAL ");
            sql.appendSql("FROM AD_DEPARAPROD DP ");
            sql.appendSql("WHERE DP." + busca + " = :CODPROD ");
            sql.appendSql("AND DP.ATIVO = 'S' ");
            sql.appendSql("AND :DTENTSAI BETWEEN DP.INIVIGENCIA AND DP.FINVIGENCIA ");
            sql.appendSql(") X ");
            sql.appendSql("ORDER BY MINVAL ASC");

            sql.setNamedParameter("CODEMP", codEmp);
            sql.setNamedParameter("CODLOCAL", codLocal);
            sql.setNamedParameter("CODPROD", codProd);
            sql.setNamedParameter("DTENTSAI", dtEntSai);

            rset = sql.executeQuery();

            if (rset.next()) {
                do {
                    BigDecimal seq = rset.getBigDecimal("SEQ");
                    DynamicVO deParaVO = daoDePara.findByPK(seq);
                    BigDecimal codProdContrapartida = deParaVO.asBigDecimal(contrapartida);
                    listDeParaVO.addAll(getRegras(codProdContrapartida,dtEntSai,busca,codEmp,codLocal));
                    listDeParaVO.add(deParaVO);
                }while (rset.next());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            JdbcUtils.closeResultSet(rset);
            NativeSql.releaseResources(sql);
            JdbcWrapper.closeSession(jdbc);
            JapeSession.close(hnd);
        }
        return listDeParaVO;
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

    private void duplicarItem(DynamicVO itemVO, BigDecimal codprod, BigDecimal qtdAtendida) {
        BigDecimal nunota = itemVO.asBigDecimal("NUNOTA");
        JapeWrapper daoItem = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
        try {
            FluidCreateVO newItem = Duplicate.duplicate(daoItem, itemVO);
            BigDecimal nextSeqItem = getSeqItem(nunota);
            newItem.set("SEQUENCIA", nextSeqItem);
            newItem.set("CODPROD", codprod);
            newItem.set("QTDNEG", qtdAtendida);
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
            String localSep = parceiroVO.asString("AD_LOCALSEPARACAO");
            if (localSep == null) {
                localSep = "1";
            }
            return localSep;
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

    private boolean parcFranqueado(BigDecimal codparc) {
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

    private BigDecimal getQtdAtendida(BigDecimal codprod, BigDecimal codlocal, String localSep, BigDecimal qtdneg) throws MGEModelException {
        BigDecimal multiplicador = getMultiplo(codprod);
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
