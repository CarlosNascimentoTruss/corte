/*    */ package br.com.sankhya.truss.evolvesolucoes.truss;
/*    */ 
/*    */ import br.com.sankhya.jape.EntityFacade;
/*    */ import br.com.sankhya.jape.dao.JdbcWrapper;
/*    */ import br.com.sankhya.modelcore.MGEModelException;
/*    */ import br.com.sankhya.modelcore.auth.AuthenticationInfo;
/*    */ import br.com.sankhya.modelcore.comercial.util.print.PrintManager;
/*    */ import br.com.sankhya.modelcore.comercial.util.print.converter.PrintConversionService;
/*    */ import br.com.sankhya.modelcore.comercial.util.print.model.PrintInfo;
/*    */ import br.com.sankhya.modelcore.util.EntityFacadeFactory;
/*    */ import br.com.sankhya.modelcore.util.MGECoreParameter;
/*    */ import br.com.sankhya.modelcore.util.Report;
/*    */ import br.com.sankhya.modelcore.util.ReportManager;
/*    */ import br.com.sankhya.sps.enumeration.DocTaste;
/*    */ import br.com.sankhya.sps.enumeration.DocType;
/*    */ import com.sankhya.util.StringUtils;
/*    */ import java.math.BigDecimal;
/*    */ import java.util.HashMap;
/*    */ import java.util.Map;
/*    */ import net.sf.jasperreports.engine.JasperPrint;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class ImpressaoRelatorioHelper
/*    */ {
/*    */   public static void imprimirRelatorio(BigDecimal nuRfe, Map<String, Object> pk) throws Exception {
/* 29 */     EntityFacade dwf = EntityFacadeFactory.getDWFFacade();
/* 30 */     JdbcWrapper jdbc = dwf.getJdbcWrapper();
/*    */ 
/*    */     
/*    */     try {
/* 34 */       jdbc.openSession();
/*    */       
/* 36 */       Map<String, Object> reportParams = buildReportParams(dwf, pk);
/*    */       
/* 38 */       Report report = ReportManager.getInstance().getReport(nuRfe, dwf);
/*    */       
/* 40 */       JasperPrint jasperPrint = report.buildJasperPrint(reportParams, jdbc.getConnection());
/*    */       
/* 42 */       byte[] conteudo = (byte[])PrintConversionService.getInstance().convert(jasperPrint, byte[].class);
/*    */       
/* 44 */       PrintManager printManager = PrintManager.getInstance();
/*    */       
/* 46 */       AuthenticationInfo authInfo = AuthenticationInfo.getCurrent();
/*    */       
/* 48 */       BigDecimal userId = authInfo.getUserID();
/* 49 */       String userName = authInfo.getName();
/* 50 */       String jobDescription = jasperPrint.getName();
/*    */       
/* 52 */       PrintInfo printInfo = new PrintInfo();
/* 53 */       printInfo.setCopies(1);
/* 54 */       printInfo.setDocument(conteudo);
/* 55 */       printInfo.setDocTaste(DocTaste.JASPER);
/* 56 */       printInfo.setDocType(DocType.RELATORIO);
/* 57 */       printInfo.setLocalPrinterName("SEM IMPRESSORA");
/* 58 */       printInfo.setJobDescription(jobDescription);
/* 59 */       printInfo.setUserId(userId);
/* 60 */       printInfo.setUserName(userName);
/*    */       
/* 62 */       printManager.print(printInfo);
/*    */     }
/* 64 */     catch (Exception e) {
/* 65 */       MGEModelException.throwMe(e);
/*    */     } finally {
/* 67 */       jdbc.closeSession();
/*    */     } 
/*    */   }
/*    */   
/*    */   private static Map<String, Object> buildReportParams(EntityFacade dwf, Map<String, Object> pk) throws Exception {
/* 72 */     Map<String, Object> reportParams = new HashMap<>();
/*    */     
/* 74 */     String pastaModelos = 
/* 75 */       StringUtils.getEmptyAsNull((String)MGECoreParameter.getParameter("os.diretorio.modelos"));
/*    */     
/* 77 */     reportParams.put("REPORT_CONNECTION", dwf.getJdbcWrapper().getConnection());
/* 78 */     reportParams.put("PDIR_MODELO", StringUtils.getEmptyAsNull(pastaModelos));
/* 79 */     reportParams.put("PCODUSULOGADO", AuthenticationInfo.getCurrent().getUserID());
/* 80 */     reportParams.put("PNOMEUSULOGADO", AuthenticationInfo.getCurrent().getName());
/*    */     
/* 82 */     for (Map.Entry<String, Object> entry : pk.entrySet()) {
/* 83 */       reportParams.put("PK_" + (String)entry.getKey(), entry.getValue());
/*    */     }
/*    */     
/* 86 */     return reportParams;
/*    */   }
/*    */ }


/* Location:              C:\Users\daniel.ratkevicius_s\Desktop\gerarroyalties.jar!\br\com\evolvesolucoes\truss\ImpressaoRelatorioHelper.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */