package br.com.sankhya.truss.corte.depara.actions;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import com.sankhya.util.JdbcUtils;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Collection;

public class ImportarCSV implements AcaoRotinaJava {
    @Override
    public void doAction(ContextoAcao ctx) throws Exception {
        Registro[] linhas = ctx.getLinhas();
        if (linhas.length != 1) {
            ctx.mostraErro("Selecione 1 registro.");
        }

        Registro registro = linhas[0];
        BigDecimal seq = (BigDecimal) registro.getCampo("SEQ");

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

            sql.appendSql("SELECT ARQUIVO FROM AD_DEPARAIMP WHERE SEQ = :SEQ");

            sql.setNamedParameter("SEQ", seq);

            rset = sql.executeQuery();

            if (rset.next()) {
                byte[] bytes = rset.getBytes("ARQUIVO");

                // Converter para String
                String conteudo = new String(bytes, StandardCharsets.UTF_8);

                // Extrair metadados
                int startMeta = conteudo.indexOf("__start_fileinformation__");
                int endMeta = conteudo.indexOf("__end_fileinformation__");

                if (startMeta == -1 || endMeta == -1) {
                    ctx.mostraErro("Metadados não encontrados.");
                }

                String metadata = conteudo.substring(
                        startMeta + "__start_fileinformation__".length(),
                        endMeta
                );

                // Verificar se é CSV
                if (!metadata.contains("\"type\":\"text/csv\"") && !metadata.contains("\"type\":\"application/octet-stream\"")) {
                    ctx.mostraErro("Não é um arquivo CSV.");
                }

                // Extrair conteúdo do CSV
                String csvData = conteudo.substring(endMeta + "__end_fileinformation__".length());

                // Processar linhas
                try (BufferedReader reader = new BufferedReader(new StringReader(csvData))) {
                    String linha;
                    boolean primeiraLinha = true;

                    while ((linha = reader.readLine()) != null) {
                        // Ignorar cabeçalho
                        if (primeiraLinha) {
                            primeiraLinha = false;
                            continue;
                        }

                        String[] colunas = linha.split(";");

                        // Verificar se tem pelo menos 3 colunas
                        if (colunas.length >= 4) {
                            String de = colunas[0].trim();
                            String para = colunas[1].trim();
                            String iniVigencia = colunas[2].trim();
                            String finVigencia = colunas[3].trim();
                            String motivo;
                            if(colunas.length >= 5) {
                                motivo = colunas[4].trim();
                            }else{
                                motivo = null;
                            }

                            validaDePara(de, para, iniVigencia, finVigencia, motivo, ctx);

                        } else {
                            ctx.mostraErro("Linha inválida: " + linha);
                        }
                    }
                }

            } else {
                ctx.mostraErro("Nenhum dado encontrado.");
            }

        } catch (Exception e) {
            MGEModelException.throwMe(e);
        } finally {
            JdbcUtils.closeResultSet(rset);
            NativeSql.releaseResources(sql);
            JdbcWrapper.closeSession(jdbc);
            JapeSession.close(hnd);
        }

        ctx.setMensagemRetorno("Importação executada com sucesso!");
    }

    private void validaDePara(String de, String para, String iniVigencia, String finVigencia, String motivo, ContextoAcao ctx) throws Exception {
        BigDecimal deBD = null;
        BigDecimal paraBD = null;
        Timestamp iniVigenciaTS = null;
        Timestamp finVigenciaTS = null;

        if (isBigDecimal(de)) {
            deBD = new BigDecimal(de);
        } else {
            ctx.mostraErro("Coluna DE não é um valor numérico: " + de);
        }

        if (isBigDecimal(para)) {
            paraBD = new BigDecimal(para);
        } else {
            ctx.mostraErro("Coluna PARA não é um valor numérico: " + para);
        }

        if (isDataValida(iniVigencia)) {
            iniVigenciaTS = converterParaTimestamp(iniVigencia);
        } else {
            ctx.mostraErro("Coluna Início da Vigência não está no formato de data dd/mm/yyyy: " + iniVigencia);
        }

        if (isDataValida(finVigencia)) {
            finVigenciaTS = converterParaTimestamp(finVigencia);
        } else {
            ctx.mostraErro("Coluna Final da Vigência não está no formato de data dd/mm/yyyy: " + finVigencia);
        }

        registraDePara(deBD, paraBD, iniVigenciaTS, finVigenciaTS, motivo, ctx);
    }

    private void registraDePara(BigDecimal deBD, BigDecimal paraBD, Timestamp iniVigenciaTS, Timestamp finVigenciaTS, String motivo, ContextoAcao ctx) throws Exception {
        BigDecimal seq = getSeq();
        FluidCreateVO fluidCreateVO = JapeFactory.dao("AD_DEPARAPROD").create();
        fluidCreateVO.set("DE", deBD);
        fluidCreateVO.set("PARA", paraBD);
        fluidCreateVO.set("INIVIGENCIA", iniVigenciaTS);
        fluidCreateVO.set("FINVIGENCIA", finVigenciaTS);
        fluidCreateVO.set("MOTIVO", motivo);
        fluidCreateVO.set("IMPORTADO", "S");
        fluidCreateVO.set("SEQ", seq);

        try {
            fluidCreateVO.save();
            BigDecimal seqImp = (BigDecimal) ctx.getLinhas()[0].getCampo("SEQ");
            FluidUpdateVO updateDeParaProd = JapeFactory.dao("AD_DEPARAIMP").prepareToUpdateByPK(seqImp);
            updateDeParaProd.set("CODUSUPROC",ctx.getUsuarioLogado());
            updateDeParaProd.set("DHPROC",new Timestamp(System.currentTimeMillis()));
            updateDeParaProd.update();
        } catch (Exception e) {
            ctx.mostraErro(e.getMessage());
        }
    }

    private BigDecimal getSeq() {
        try {
            Collection<DynamicVO> collection = JapeFactory.dao("AD_DEPARAPROD").find(" SEQ = (SELECT MAX(SEQ) FROM AD_DEPARAPROD)");

            if (collection.isEmpty()) {
                return BigDecimal.ONE;
            } else {
                DynamicVO deParaVO = collection.iterator().next();
                BigDecimal lastSeqLog = deParaVO.asBigDecimal("SEQ");
                return lastSeqLog.add(BigDecimal.ONE);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isBigDecimal(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return false;
        }
        try {
            new java.math.BigDecimal(valor.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean isDataValida(String data) {
        if (data == null || data.trim().isEmpty()) {
            return false;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/uuuu")
                .withResolverStyle(ResolverStyle.STRICT);
        try {
            LocalDate.parse(data.trim(), formatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public Timestamp converterParaTimestamp(String data) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/uuuu")
                .withResolverStyle(ResolverStyle.STRICT);
        LocalDate localDate = LocalDate.parse(data.trim(), formatter);
        return Timestamp.valueOf(localDate.atStartOfDay());
    }
}
