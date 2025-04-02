/*     */ package br.com.evolvesolucoes.truss;
/*     */ 
/*     */ import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
/*     */ import br.com.sankhya.extensions.actionbutton.ContextoAcao;
/*     */ import br.com.sankhya.extensions.actionbutton.Registro;
/*     */ import br.com.sankhya.jape.EntityFacade;
/*     */ import br.com.sankhya.jape.bmp.PersistentLocalEntity;
/*     */ import br.com.sankhya.jape.dao.JdbcWrapper;
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
/*     */ public class gerarDocForn
/*     */   implements AcaoRotinaJava
/*     */ {
/*     */   public void doAction(ContextoAcao ctx) throws Exception {
/*  31 */     JdbcWrapper jdbc = null;
/*     */     
/*  33 */     Registro[] lancamentos = ctx.getLinhas(); byte b; int i;
/*     */     Registro[] arrayOfRegistro1;
/*  35 */     for (i = (arrayOfRegistro1 = lancamentos).length, b = 0; b < i; ) { Registro registro = arrayOfRegistro1[b];
/*     */       
/*     */       try {
/*  38 */         EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
/*  39 */         jdbc = dwfEntityFacade.getJdbcWrapper();
/*  40 */         jdbc.openSession();
/*     */         
/*  42 */         BigDecimal NumRel = BigDecimal.valueOf(58L);
/*     */         
/*  44 */         Map<String, Object> pk = new HashMap<>();
/*     */         
/*  46 */         pk.put("NUNOTA", registro.getCampo("NUNOTA"));
/*     */         
/*  48 */         Map<String, Object> reportParams = buildReportParams(dwfEntityFacade, pk);
/*     */         
/*  50 */         Report report = ReportManager.getInstance().getReport(NumRel, dwfEntityFacade);
/*  51 */         report.setUseVirtualization(false);
/*     */ 
/*     */ 
/*     */         
/*  55 */         byte[] conteudo = report.fill(reportParams, jdbc.getConnection());
/*     */         
/*  57 */         String mimeType = "application/pdf";
/*     */         
/*  59 */         DynamicVO centralParamVO = (DynamicVO)dwfEntityFacade
/*  60 */           .findEntityByPrimaryKeyAsVO("AD_CENTRALPARAM", BigDecimal.ONE);
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
/*  76 */         DynamicVO anexoVO = (DynamicVO)dwfEntityFacade.getDefaultValueObjectInstance("AnexoMensagem");
/*     */         
/*  78 */         anexoVO.setProperty("NOMEARQUIVO", String.valueOf("teste.pdf"));
/*  79 */         anexoVO.setProperty("TIPO", mimeType);
/*  80 */         anexoVO.setProperty("ANEXO", conteudo);
/*     */         
/*  82 */         PersistentLocalEntity anexoEntity = dwfEntityFacade.createEntity("AnexoMensagem", (EntityVO)anexoVO);
/*  83 */         anexoVO = (DynamicVO)anexoEntity.getValueObject();
/*     */         
/*  85 */         BigDecimal nuAnexo = anexoVO.asBigDecimal("NUANEXO");
/*     */         
/*  87 */         System.out.println("anexo: " + nuAnexo);
/*     */         
/*  89 */         DynamicVO filaVO = (DynamicVO)dwfEntityFacade.getDefaultValueObjectInstance("MSDFilaMensagem");
/*     */         
/*  91 */         filaVO.setProperty("EMAIL", String.valueOf("gyvago.ribeiro@sankhya.com.br"));
/*  92 */         filaVO.setProperty("CODCON", BigDecimal.ZERO);
/*  93 */         filaVO.setProperty("CODMSG", null);
/*  94 */         filaVO.setProperty("STATUS", "Pendente");
/*  95 */         filaVO.setProperty("TIPOENVIO", "E");
/*  96 */         filaVO.setProperty("TENTENVIO", BigDecimal.ONE);
/*  97 */         filaVO.setProperty("MAXTENTENVIO", BigDecimalUtil.valueOf(3L));
/*  98 */         filaVO.setProperty("ASSUNTO", String.valueOf("teste anexo"));
/*  99 */         filaVO.setProperty("MENSAGEM", (
/* 100 */             new String("teste anexo".toString().getBytes("ISO-8859-1"), "ISO-8859-1")).toCharArray());
/* 101 */         filaVO.setProperty("CODUSUREMET", AuthenticationInfo.getCurrent().getUserID());
/*     */         
/* 103 */         PersistentLocalEntity filaEntity = dwfEntityFacade.createEntity("MSDFilaMensagem", (EntityVO)filaVO);
/* 104 */         filaVO = (DynamicVO)filaEntity.getValueObject();
/*     */         
/* 106 */         BigDecimal codFila = filaVO.asBigDecimal("CODFILA");
/*     */         
/* 108 */         DynamicVO anexoPorMsgVO = (DynamicVO)dwfEntityFacade.getDefaultValueObjectInstance("AnexoPorMensagem");
/*     */         
/* 110 */         anexoPorMsgVO.setProperty("CODFILA", codFila);
/* 111 */         anexoPorMsgVO.setProperty("NUANEXO", nuAnexo);
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
/* 123 */       catch (Exception e) {
/* 124 */         e.printStackTrace();
/* 125 */         System.out.println("Erro ao enviar e-mail docforn");
/*     */         return;
/*     */       } finally {
/* 128 */         JdbcWrapper.closeSession(jdbc);
/*     */       } 
/* 130 */       JdbcWrapper.closeSession(jdbc);
/*     */       b++; }
/*     */   
/*     */   }
/*     */   
/*     */   private static Map<String, Object> buildReportParams(EntityFacade dwf, Map<String, Object> pk) throws Exception {
/* 136 */     Map<String, Object> reportParams = new HashMap<>();
/*     */     
/* 138 */     String pastaModelos = 
/* 139 */       StringUtils.getEmptyAsNull((String)MGECoreParameter.getParameter("os.diretorio.modelos"));
/*     */     
/* 141 */     reportParams.put("REPORT_CONNECTION", dwf.getJdbcWrapper().getConnection());
/* 142 */     reportParams.put("PDIR_MODELO", StringUtils.getEmptyAsNull(pastaModelos));
/* 143 */     reportParams.put("PCODUSULOGADO", AuthenticationInfo.getCurrent().getUserID());
/* 144 */     reportParams.put("PNOMEUSULOGADO", AuthenticationInfo.getCurrent().getName());
/*     */     
/* 146 */     for (Map.Entry<String, Object> entry : pk.entrySet()) {
/* 147 */       reportParams.put("PK_" + (String)entry.getKey(), entry.getValue());
/*     */     }
/*     */     
/* 150 */     return reportParams;
/*     */   }
/*     */ }


/* Location:              C:\Users\daniel.ratkevicius_s\Desktop\gerarroyalties.jar!\br\com\evolvesolucoes\truss\gerarDocForn.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */