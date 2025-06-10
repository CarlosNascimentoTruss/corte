package br.com.sankhya.truss.corte.depara.events;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.ws.ServiceContext;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;

public class DePara implements EventoProgramavelJava {

    private void registraLog(PersistenceEvent event, String quando) throws Exception {
        BigDecimal seqlog = getSeqLog();
        BigDecimal codUsu = getUsuarioLogado();
        Timestamp dhAlter = new Timestamp(System.currentTimeMillis());

        BigDecimal deOld = null;
        BigDecimal paraOld = null;
        Timestamp iniVigenciaOld = null;
        Timestamp finVigenciaOld = null;
        String motivoOld = null;

        BigDecimal deNew = null;
        BigDecimal paraNew = null;
        Timestamp iniVigenciaNew = null;
        Timestamp finVigenciaNew = null;
        String motivoNew = null;

        if (quando.equals("UPDATE")) {
            DynamicVO oldVO = (DynamicVO) event.getOldVO();
            deOld = oldVO.asBigDecimal("DE");
            paraOld = oldVO.asBigDecimal("PARA");
            iniVigenciaOld = oldVO.asTimestamp("INIVIGENCIA");
            finVigenciaOld = oldVO.asTimestamp("FINVIGENCIA");
            motivoOld = oldVO.asString("MOTIVO");
        }

        if (quando.equals("DELETE")) {
            DynamicVO vo = (DynamicVO) event.getVo();
            deOld = vo.asBigDecimal("DE");
            paraOld = vo.asBigDecimal("PARA");
            iniVigenciaOld = vo.asTimestamp("INIVIGENCIA");
            finVigenciaOld = vo.asTimestamp("FINVIGENCIA");
            motivoOld = vo.asString("MOTIVO");
        }

        if (quando.equals("INSERT") || quando.equals("UPDATE")) {
            DynamicVO newVO = (DynamicVO) event.getVo();
            deNew = newVO.asBigDecimal("DE");
            paraNew = newVO.asBigDecimal("PARA");
            iniVigenciaNew = newVO.asTimestamp("INIVIGENCIA");
            finVigenciaNew = newVO.asTimestamp("FINVIGENCIA");
            motivoNew = newVO.asString("MOTIVO");
        }

        FluidCreateVO fluidCreateVO = JapeFactory.dao("AD_DEPARAPRODLOG").create();

        fluidCreateVO.set("SEQLOG", seqlog);
        fluidCreateVO.set("DEOLD", deOld);
        fluidCreateVO.set("DENEW", deNew);
        fluidCreateVO.set("PARAOLD", paraOld);
        fluidCreateVO.set("PARANEW", paraNew);
        fluidCreateVO.set("INIVIGENCIAOLD", iniVigenciaOld);
        fluidCreateVO.set("INIVIGENCIANEW", iniVigenciaNew);
        fluidCreateVO.set("FINVIGENCIAOLD", finVigenciaOld);
        fluidCreateVO.set("FINVIGENCIANEW", finVigenciaNew);
        fluidCreateVO.set("MOTIVOOLD", motivoOld);
        fluidCreateVO.set("MOTIVONEW", motivoNew);
        fluidCreateVO.set("CODUSU", codUsu);
        fluidCreateVO.set("DHALTER", dhAlter);
        fluidCreateVO.set("OPERACAO", quando);

        fluidCreateVO.save();

    }

    private BigDecimal getUsuarioLogado() {
        return ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID();
    }

    private BigDecimal getSeqLog() throws Exception {
        Collection<DynamicVO> collection = JapeFactory.dao("AD_DEPARAPRODLOG").find(" SEQLOG = (SELECT MAX(SEQLOG) FROM AD_DEPARAPRODLOG)");
        if (collection.isEmpty()) {
            return BigDecimal.ONE;
        } else {
            DynamicVO logVO = collection.iterator().next();
            BigDecimal lastSeqLog = logVO.asBigDecimal("SEQLOG");
            return lastSeqLog.add(BigDecimal.ONE);
        }
    }

    private void validaRegistro(PersistenceEvent event) throws Exception {
        DynamicVO voNew = (DynamicVO) event.getVo();

        BigDecimal de = voNew.asBigDecimal("DE");
        BigDecimal para = voNew.asBigDecimal("PARA");
        Timestamp iniVigencia = voNew.asTimestamp("INIVIGENCIA");
        Timestamp finVigencia = voNew.asTimestamp("FINVIGENCIA");
        BigDecimal seq = voNew.asBigDecimal("SEQ");

        validaProdutosRepetidos(de, para);
        validaProdutosEspelhados(de, para, iniVigencia, finVigencia);
        validaDataInicialVersusFinal(iniVigencia, finVigencia, de, para);
        validaDatasConflitantes(iniVigencia, finVigencia, de, para, seq);

    }

    private void validaProdutosEspelhados(BigDecimal de, BigDecimal para, Timestamp iniVigencia, Timestamp finVigencia) {
        try {
            Collection<DynamicVO> collection = JapeFactory.dao("AD_DEPARAPROD").find(" DE = ? AND PARA = ? AND INIVIGENCIA = ? AND FINVIGENCIA = ?", para, de, iniVigencia, finVigencia);
            if (!collection.isEmpty()) {
                throw new MGEModelException("Produto esperalhado nesta data de vigencia: " + de + " = " + para);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void validaDatasConflitantes(Timestamp iniVigencia, Timestamp finVigencia, BigDecimal de, BigDecimal para, BigDecimal seq) throws Exception {
        Collection<DynamicVO> collection = JapeFactory.dao("AD_DEPARAPROD").find(" DE = ? AND PARA = ? AND SEQ <> ?", de, para, seq);
        for (DynamicVO vo : collection) {
            Timestamp iniVigenciaDaBusca = vo.asTimestamp("INIVIGENCIA");
            Timestamp finVigenciaDaBusca = vo.asTimestamp("FINVIGENCIA");

            if (iniVigencia.compareTo(iniVigenciaDaBusca) >= 0 && iniVigencia.compareTo(finVigenciaDaBusca) <= 0) {
                throw new MGEModelException("Início da vigência está em conflito com outro registro: " + de + " = " + para);
            }

            if (finVigencia.compareTo(iniVigenciaDaBusca) >= 0 && finVigencia.compareTo(finVigenciaDaBusca) <= 0) {
                throw new MGEModelException("Final da vigência está em conflito com outro registro: " + de + " = " + para);
            }

            if (iniVigencia.compareTo(iniVigenciaDaBusca) <= 0 && finVigencia.compareTo(finVigenciaDaBusca) >= 0) {
                throw new MGEModelException("Vigência está em conflito com outro registro: " + de + " = " + para);
            }
        }
    }

    private void validaDataInicialVersusFinal(Timestamp iniVigencia, Timestamp finVigencia, BigDecimal de, BigDecimal para) throws MGEModelException {
        if (iniVigencia.after(finVigencia)) {
            throw new MGEModelException("Vigência final não pode ser menor que inicial: " + de + " = " + para);
        }
    }

    private void validaProdutosRepetidos(BigDecimal de, BigDecimal para) throws MGEModelException {
        if (de.compareTo(para) == 0) {
            throw new MGEModelException("Produto DE não pode ser igual ao produto PARA: " + de + " = " + para);
        }
    }

    @Override
    public void beforeInsert(PersistenceEvent event) throws Exception {
        validaRegistro(event);
        registraLog(event, "INSERT");
    }

    @Override
    public void beforeUpdate(PersistenceEvent event) throws Exception {
        validaRegistro(event);
        registraLog(event, "UPDATE");
    }

    @Override
    public void beforeDelete(PersistenceEvent event) throws Exception {
        registraLog(event, "DELETE");
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
}
