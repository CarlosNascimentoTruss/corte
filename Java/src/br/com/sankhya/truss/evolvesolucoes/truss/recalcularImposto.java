/*     */ package br.com.evolvesolucoes.truss;
/*     */ 
/*     */ import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
/*     */ import br.com.sankhya.extensions.actionbutton.ContextoAcao;
/*     */ import br.com.sankhya.extensions.actionbutton.Registro;
/*     */ import br.com.sankhya.jape.EntityFacade;
/*     */ import br.com.sankhya.jape.bmp.PersistentLocalEntity;
/*     */ import br.com.sankhya.jape.core.JapeSession;
/*     */ import br.com.sankhya.jape.dao.JdbcWrapper;
/*     */ import br.com.sankhya.jape.sql.NativeSql;
/*     */ import br.com.sankhya.jape.util.JapeSessionContext;
/*     */ import br.com.sankhya.jape.vo.DynamicVO;
/*     */ import br.com.sankhya.jape.vo.EntityVO;
/*     */ import br.com.sankhya.modelcore.auth.AuthenticationInfo;
/*     */ import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
/*     */ import br.com.sankhya.modelcore.util.EntityFacadeFactory;
/*     */ import br.com.sankhya.modelcore.util.LockTableUtils;
/*     */ import java.math.BigDecimal;
/*     */ import java.sql.ResultSet;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class recalcularImposto
/*     */   implements AcaoRotinaJava
/*     */ {
/*     */   public void doAction(ContextoAcao ctx) throws Exception {
/*  33 */     JdbcWrapper jdbc = null;
/*  34 */     JapeSession.SessionHandle hdn = null;
/*  35 */     final EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
/*     */     
/*  37 */     Registro[] lancamentos = ctx.getLinhas(); byte b; int i;
/*     */     Registro[] arrayOfRegistro1;
/*  39 */     for (i = (arrayOfRegistro1 = lancamentos).length, b = 0; b < i; ) { Registro registro = arrayOfRegistro1[b];
/*     */       
/*     */       try {
/*  42 */         hdn = JapeSession.getCurrentSession().getTopMostHandle();
/*  43 */         jdbc = dwfEntityFacade.getJdbcWrapper();
/*  44 */         setupContext();
/*     */         
/*  46 */         NativeSql queNotas = new NativeSql(jdbc);
/*     */         
/*  48 */         queNotas.appendSql(
/*  49 */             "SELECT CAB.NUNOTA FROM TGFCAB CAB, TGFTOP TOP WHERE CAB.CODTIPOPER = TOP.CODTIPOPER AND CAB.DHTIPOPER = TOP.DHALTER AND CAB.NUNOTA=" + 
/*  50 */             registro.getCampo("NUNOTA"));
/*     */         
/*  52 */         final ResultSet rsFazerfinJob = queNotas.executeQuery();
/*     */         
/*  54 */         if (rsFazerfinJob.next()) {
/*     */           do {
/*  56 */             hdn.execWithTX(new JapeSession.TXBlock()
/*     */                 {
/*     */                   
/*     */                   public void doWithTx() throws Exception
/*     */                   {
/*  61 */                     BigDecimal nuNota = rsFazerfinJob.getBigDecimal("NUNOTA");
/*  62 */                     LockTableUtils.lockNotaBD(nuNota);
/*     */                     
/*  64 */                     PersistentLocalEntity cabEntity = dwfEntityFacade.findEntityByPrimaryKey(
/*  65 */                         "CabecalhoNota", new Object[] { nuNota });
/*  66 */                     DynamicVO cabVO = (DynamicVO)cabEntity.getValueObject();
/*     */                     
/*     */                     try {
/*  69 */                       ImpostosHelpper impostoHelper = new ImpostosHelpper();
/*     */                       
/*  71 */                       impostoHelper.carregarNota(nuNota);
/*  72 */                       impostoHelper.atualizaCFO(nuNota);
/*  73 */                       impostoHelper.setForcarRecalculo(true);
/*  74 */                       impostoHelper.calculaICMS(true);
/*     */ 
/*     */                       
/*  77 */                       cabEntity.setValueObject((EntityVO)cabVO);
/*     */                     }
/*  79 */                     catch (Exception e) {
/*     */                       
/*  81 */                       cabEntity.setValueObject((EntityVO)cabVO);
/*     */                     } 
/*     */                   }
/*     */                 });
/*     */ 
/*     */             
/*  87 */             Thread.sleep(100L);
/*  88 */           } while (rsFazerfinJob.next());
/*     */         }
/*     */         
/*  91 */         rsFazerfinJob.close();
/*     */       }
/*  93 */       catch (Exception e) {
/*  94 */         e.printStackTrace();
/*     */       } finally {
/*     */         
/*  97 */         JdbcWrapper.closeSession(jdbc);
/*     */       } 
/*     */       b++; }
/*     */   
/*     */   }
/*     */   
/*     */   private void setupContext() {
/* 104 */     AuthenticationInfo auth = AuthenticationInfo.getCurrent();
/* 105 */     JapeSessionContext.putProperty("usuario_logado", auth.getUserID());
/* 106 */     JapeSessionContext.putProperty("authInfo", auth);
/* 107 */     JapeSessionContext.putProperty("br.com.sankhya.com.CentralCompraVenda", Boolean.TRUE);
/* 108 */     JapeSessionContext.putProperty("br.com.sankhya.com.CentralCompraVenda", Boolean.TRUE);
/* 109 */     JapeSession.putProperty("ItemNota.incluindo.alterando.pela.central", Boolean.TRUE);
/*     */   }
/*     */ }


/* Location:              C:\Users\daniel.ratkevicius_s\Desktop\gerarroyalties.jar!\br\com\evolvesolucoes\truss\recalcularImposto.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */