/*     */ package br.com.evolvesolucoes.truss;
/*     */ 
/*     */ import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
/*     */ import br.com.sankhya.jape.EntityFacade;
/*     */ import br.com.sankhya.jape.bmp.PersistentLocalEntity;
/*     */ import br.com.sankhya.jape.dao.JdbcWrapper;
/*     */ import br.com.sankhya.jape.event.PersistenceEvent;
/*     */ import br.com.sankhya.jape.event.TransactionContext;
/*     */ import br.com.sankhya.jape.vo.DynamicVO;
/*     */ import br.com.sankhya.jape.vo.EntityVO;
/*     */ import br.com.sankhya.modelcore.auth.AuthenticationInfo;
/*     */ import br.com.sankhya.modelcore.util.EntityFacadeFactory;
/*     */ import br.com.sankhya.modelcore.util.MGECoreParameter;
/*     */ import br.com.sankhya.modelcore.util.Report;
/*     */ import br.com.sankhya.modelcore.util.ReportManager;
/*     */ import com.sankhya.util.BigDecimalUtil;
/*     */ import com.sankhya.util.StringUtils;
/*     */ import java.math.BigDecimal;
/*     */ import java.util.HashMap;
/*     */ import java.util.Map;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class validarAlteracaoPedidoGerado
/*     */   implements EventoProgramavelJava
/*     */ {
/*     */   public void afterDelete(PersistenceEvent arg0) throws Exception {}
/*     */   
/*     */   public void afterInsert(PersistenceEvent arg0) throws Exception {}
/*     */   
/*     */   public void afterUpdate(PersistenceEvent arg0) throws Exception {}
/*     */   
/*     */   public void beforeCommit(TransactionContext arg0) throws Exception {}
/*     */   
/*     */   public void beforeDelete(PersistenceEvent arg0) throws Exception {}
/*     */   
/*     */   public void beforeInsert(PersistenceEvent arg0) throws Exception {}
/*     */   
/*     */   public void beforeUpdate(PersistenceEvent event) throws Exception {
/*  66 */     EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
/*     */     
/*  68 */     DynamicVO newVO = (DynamicVO)event.getVo();
/*  69 */     DynamicVO oldVO = (DynamicVO)event.getOldVO();
/*     */     
/*  71 */     BigDecimal nunotaNew = newVO.asBigDecimal("NUNOTA");
/*  72 */     BigDecimal nunotaOld = oldVO.asBigDecimal("NUNOTA");
/*     */     
/*  74 */     String statusNew = newVO.asString("STATUSNOTA");
/*  75 */     String statusOld = oldVO.asString("STATUSNOTA");
/*     */     
/*  77 */     if ("L".equals(statusNew) && !"L".equals(statusOld) && "O".equals(newVO.getProperty("TIPMOV"))) {
/*     */       
/*  79 */       JdbcWrapper jdbc = null;
/*     */ 
/*     */       
/*     */       try {
/*  83 */         jdbc = dwfEntityFacade.getJdbcWrapper();
/*  84 */         jdbc.openSession();
/*     */         
/*  86 */         BigDecimal NumRel = BigDecimal.valueOf(58L);
/*     */         
/*  88 */         Map<String, Object> pk = new HashMap<>();
/*     */         
/*  90 */         pk.put("NUNOTA", newVO.getProperty("NUNOTA"));
/*     */         
/*  92 */         Map<String, Object> reportParams = buildReportParams(dwfEntityFacade, pk);
/*     */         
/*  94 */         Report report = ReportManager.getInstance().getReport(NumRel, dwfEntityFacade);
/*  95 */         report.setUseVirtualization(false);
/*     */ 
/*     */ 
/*     */         
/*  99 */         byte[] conteudo = report.fill(reportParams, jdbc.getConnection());
/*     */         
/* 101 */         String mimeType = "application/pdf";
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */         
/* 124 */         DynamicVO anexoVO = (DynamicVO)dwfEntityFacade.getDefaultValueObjectInstance("AnexoMensagem");
/*     */         
/* 126 */         anexoVO.setProperty("NOMEARQUIVO", String.valueOf("teste.pdf"));
/* 127 */         anexoVO.setProperty("TIPO", mimeType);
/* 128 */         anexoVO.setProperty("ANEXO", conteudo);
/*     */         
/* 130 */         PersistentLocalEntity anexoEntity = dwfEntityFacade.createEntity("AnexoMensagem", (EntityVO)anexoVO);
/* 131 */         anexoVO = (DynamicVO)anexoEntity.getValueObject();
/*     */         
/* 133 */         BigDecimal nuAnexo = anexoVO.asBigDecimal("NUANEXO");
/*     */         
/* 135 */         System.out.println("anexo: " + nuAnexo);
/*     */         
/* 137 */         DynamicVO filaVO = (DynamicVO)dwfEntityFacade.getDefaultValueObjectInstance("MSDFilaMensagem");
/*     */         
/* 139 */         filaVO.setProperty("EMAIL", String.valueOf("gyvago.ribeiro@sankhya.com.br"));
/* 140 */         filaVO.setProperty("CODCON", BigDecimal.ZERO);
/* 141 */         filaVO.setProperty("CODMSG", null);
/* 142 */         filaVO.setProperty("STATUS", "Pendente");
/* 143 */         filaVO.setProperty("TIPOENVIO", "E");
/* 144 */         filaVO.setProperty("TENTENVIO", BigDecimal.ONE);
/* 145 */         filaVO.setProperty("MAXTENTENVIO", BigDecimalUtil.valueOf(3L));
/* 146 */         filaVO.setProperty("ASSUNTO", String.valueOf("teste anexo"));
/* 147 */         filaVO.setProperty("MENSAGEM", (
/* 148 */             new String("teste anexo".toString().getBytes("ISO-8859-1"), "ISO-8859-1")).toCharArray());
/* 149 */         filaVO.setProperty("CODUSUREMET", AuthenticationInfo.getCurrent().getUserID());
/*     */         
/* 151 */         PersistentLocalEntity filaEntity = dwfEntityFacade.createEntity("MSDFilaMensagem", (EntityVO)filaVO);
/* 152 */         filaVO = (DynamicVO)filaEntity.getValueObject();
/*     */         
/* 154 */         BigDecimal codFila = filaVO.asBigDecimal("CODFILA");
/*     */         
/* 156 */         DynamicVO anexoPorMsgVO = (DynamicVO)dwfEntityFacade.getDefaultValueObjectInstance("AnexoPorMensagem");
/*     */         
/* 158 */         anexoPorMsgVO.setProperty("CODFILA", codFila);
/* 159 */         anexoPorMsgVO.setProperty("NUANEXO", nuAnexo);
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */       
/*     */       }
/* 174 */       catch (Exception e) {
/* 175 */         e.printStackTrace();
/* 176 */         System.out.println("Erro ao enviar e-mail docforn");
/*     */         return;
/*     */       } finally {
/* 179 */         JdbcWrapper.closeSession(jdbc);
/*     */       } 
/* 181 */       JdbcWrapper.closeSession(jdbc);
/*     */     } 
/*     */ 
/*     */     
/* 185 */     if (nunotaNew != null && nunotaOld != null)
/*     */     {
/* 187 */       throw new Exception(String.format(getMensagem(dwfEntityFacade, "Alteração não permitida", 
/* 188 */               "Pedido de venda ja gerado para linha selecionada", "Inicie um novo lançamento"), new Object[0]));
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private String getMensagem(EntityFacade dwfFacade, String mensagem, String motivo, String solucao) throws Exception {
/* 195 */     StringBuffer messageBuf = new StringBuffer();
/*     */     
/* 197 */     messageBuf.append(
/* 198 */         "<p align=''center''><a href=\"http://www.sankhya.com.br\" target=\"_blank\"><img src=\"http://www.sankhya.com.br/imagens/logo-sankhya.png\"></img></a></p><br><br/>");
/* 199 */     messageBuf.append("  <p align=\"left\"><font size=\"12\" face=\"arial\" color=\"#000000\"><b>Atenção:  </b>");
/* 200 */     messageBuf.append(mensagem).append(".<br><br>");
/* 201 */     messageBuf.append("<b>Motivo: </b>").append(motivo).append(".<br><br>");
/* 202 */     messageBuf.append("<b>Solução: </b>").append(solucao).append(".<br><br>");
/* 203 */     messageBuf.append(
/* 204 */         "<p align=\"center\"><font size=\"10\" color=\"#008B45\"><b>Informações para o Implantador e/ou equipe Sankhya</b></font>.<br>");
/*     */     
/* 206 */     return messageBuf.toString();
/*     */   }
/*     */   
/*     */   private static Map<String, Object> buildReportParams(EntityFacade dwf, Map<String, Object> pk) throws Exception {
/* 210 */     Map<String, Object> reportParams = new HashMap<>();
/*     */     
/* 212 */     String pastaModelos = 
/* 213 */       StringUtils.getEmptyAsNull((String)MGECoreParameter.getParameter("os.diretorio.modelos"));
/*     */     
/* 215 */     reportParams.put("REPORT_CONNECTION", dwf.getJdbcWrapper().getConnection());
/* 216 */     reportParams.put("PDIR_MODELO", StringUtils.getEmptyAsNull(pastaModelos));
/* 217 */     reportParams.put("PCODUSULOGADO", AuthenticationInfo.getCurrent().getUserID());
/* 218 */     reportParams.put("PNOMEUSULOGADO", AuthenticationInfo.getCurrent().getName());
/*     */     
/* 220 */     for (Map.Entry<String, Object> entry : pk.entrySet()) {
/* 221 */       reportParams.put("PK_" + (String)entry.getKey(), entry.getValue());
/*     */     }
/*     */     
/* 224 */     return reportParams;
/*     */   }
/*     */ }


/* Location:              C:\Users\daniel.ratkevicius_s\Desktop\gerarroyalties.jar!\br\com\evolvesolucoes\truss\validarAlteracaoPedidoGerado.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */