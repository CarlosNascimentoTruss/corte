// Pacote da classe
package br.com.sankhya.truss.enviaSeparacao.actions;

// Importações necessárias para a ação, acesso a dados, SQL nativo, utilitários e manipulação de datas
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import com.sankhya.util.TimeUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

/**
 * Classe responsável por exportar dados da view AD_VWENVIOSEPARACAO,
 * gerar um arquivo CSV e registrar os envios na tabela AD_HISTENVIOSEP.
 */
public class EnviaSeparacaoAction implements AcaoRotinaJava {

    @Override
    public void doAction(ContextoAcao ctx) throws Exception {
        // Geração do nome do arquivo com data e hora atual
        String dataFormatada = new SimpleDateFormat("dd_MM_yyyy_HHmmss").format(new Date());
        String nomeArquivo = "EnvioSeparacao_" + dataFormatada + ".csv";

        // Inicialização das facades e wrappers necessários
        EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
        JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();

        JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA); // TGFCAB
        JapeWrapper histDAO = JapeFactory.dao("AD_HISTENVIOSEP"); // Tabela de histórico

        // Cabeçalho do CSV
        StringBuilder csv = new StringBuilder();
        csv.append("CdMaterial;Descrição;QtdeCaixa;QtdeUnid;lote;NrPedido;Nfe;Transportador\n");

        try {
            // Recupera os registros selecionados na tela
            Registro[] linhas = ctx.getLinhas();

            for (Registro linha : linhas) {
                // Obtém o NUNOTA de cada registro selecionado
                BigDecimal nunota = (BigDecimal) linha.getCampo("NUNOTA");

                // Consulta os itens do pedido (TGFCAB + TGFITE + TGFPRO)
                String sql = "SELECT " +
                        " ITE.CODPROD, " +
                        " PRO.DESCRPROD, " +
                        " CEIL(ITE.QTDNEG / PRO.QTDEMB) AS QTDCAIXA, " +
                        " ITE.QTDNEG, " +
                        " ITE.CONTROLE, " +
                        " SEP.NUNOTA " +
                        " FROM TGFCAB SEP " +
                        " JOIN TGFITE ITE ON ITE.NUNOTA = SEP.NUNOTA " +
                        " JOIN TGFPRO PRO ON PRO.CODPROD = ITE.CODPROD " +
                        " WHERE SEP.NUNOTA = :P_NUNOTA ";

                NativeSql query = new NativeSql(jdbc);
                query.setNamedParameter("P_NUNOTA", nunota);
                ResultSet r = query.executeQuery(sql);

                // Percorre os resultados da consulta
                while (r.next()) {
                    // Extrai os dados do ResultSet
                    String codprod = r.getBigDecimal("CODPROD").toString();
                    String descrprod = r.getString("DESCRPROD");
                    String qtdcaixa = r.getBigDecimal("QTDCAIXA").toString();
                    String qtdneg = r.getBigDecimal("QTDNEG").toString();
                    String controle = r.getString("CONTROLE");
                    String pedido = r.getBigDecimal("NUNOTA").toString();

                    // Monta linha do CSV
                    csv.append(codprod).append(";")
                            .append(descrprod).append(";")
                            .append(qtdcaixa).append(";")
                            .append(qtdneg).append(";")
                            .append(controle).append(";")
                            .append(pedido).append(";")
                            .append(";") // NFE e Transportador vazios
                            .append("\n");

                    // Registra o envio na tabela de histórico
                    histDAO.create()
                            .set("NUNOTA", nunota)
                            .set("CODPROD", new BigDecimal(codprod))
                            .set("DESCRPROD", descrprod)
                            .set("QTDCAIXA", new BigDecimal(qtdcaixa))
                            .set("QTDNEG", new BigDecimal(qtdneg))
                            .set("CONTROLE", controle)
                            .set("DHENVIO", TimeUtils.getNow()) // Data/Hora atual
                            .set("CODUSU", ctx.getUsuarioLogado()) // Usuário que executou a ação
                            .save();
                }

                // Codifica o CSV em base64 para permitir o download direto via link HTML
                String csvBase64 = Base64.getEncoder().encodeToString(csv.toString().getBytes("UTF-8"));
                String link = "<a download='" + nomeArquivo + "' href='data:text/csv;base64," + csvBase64 + "'>Clique aqui para baixar o arquivo</a>";

                // Atualiza o cabeçalho da nota com data de liberação e status do pedido
                cabDAO.prepareToUpdateByPK(nunota)
                        .set("AD_DTLIBEXP", TimeUtils.getNow()) // Campo customizado
                        .set("AD_STATUSPED", "29") // Status customizado da separação
                        .update();

                // Retorna mensagem de sucesso para o usuário com o link para download
                ctx.setMensagemRetorno("<b>Arquivo gerado com sucesso.</b><br>" + link);
            }
        } catch (Exception e) {
            // Em caso de erro, exibe a exceção
            e.printStackTrace();
            ctx.mostraErro(e.getMessage());
        }
    }
}
