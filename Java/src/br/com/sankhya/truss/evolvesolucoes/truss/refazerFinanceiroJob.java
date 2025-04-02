/*    */ package br.com.evolvesolucoes.truss;
/*    */ 
/*    */ import br.com.sankhya.jape.EntityFacade;
/*    */ import br.com.sankhya.jape.bmp.PersistentLocalEntity;
/*    */ import br.com.sankhya.jape.core.JapeSession;
/*    */ import br.com.sankhya.jape.dao.JdbcWrapper;
/*    */ import br.com.sankhya.jape.sql.NativeSql;
/*    */ import br.com.sankhya.jape.util.JapeSessionContext;
/*    */ import br.com.sankhya.jape.vo.DynamicVO;
/*    */ import br.com.sankhya.jape.vo.EntityVO;
/*    */ import br.com.sankhya.modelcore.auth.AuthenticationInfo;
/*    */ import br.com.sankhya.modelcore.comercial.CentralFinanceiro;
/*    */ import br.com.sankhya.modelcore.util.EntityFacadeFactory;
/*    */ import br.com.sankhya.modelcore.util.LockTableUtils;
/*    */ import java.math.BigDecimal;
/*    */ import java.sql.ResultSet;
/*    */ import org.cuckoo.core.ScheduledAction;
/*    */ import org.cuckoo.core.ScheduledActionContext;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class refazerFinanceiroJob
/*    */   implements ScheduledAction
/*    */ {
/*    */   public void onTime(ScheduledActionContext ctx) {
/* 33 */     JdbcWrapper jdbc = null;
/* 34 */     JapeSession.SessionHandle hdn = null;
/* 35 */     final EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
/*    */     
/*    */     try {
/* 38 */       hdn = JapeSession.getCurrentSession().getTopMostHandle();
/* 39 */       jdbc = dwfEntityFacade.getJdbcWrapper();
/* 40 */       setupContext();
/*    */       
/* 42 */       NativeSql queNotas = new NativeSql(jdbc);
/*    */       
/* 44 */       queNotas.appendSql("SELECT CAB.NUNOTA FROM TGFCAB CAB WHERE CAB.CODTIPOPER=3119 AND CAB.AD_PROCESSAFIN=2");
/*    */       
/* 46 */       final ResultSet rsFazerfinJob = queNotas.executeQuery();
/*    */       
/* 48 */       if (rsFazerfinJob.next()) {
/*    */         do {
/* 50 */           hdn.execWithTX(new JapeSession.TXBlock()
/*    */               {
/*    */                 
/*    */                 public void doWithTx() throws Exception
/*    */                 {
/* 55 */                   BigDecimal nuNota = rsFazerfinJob.getBigDecimal("NUNOTA");
/* 56 */                   LockTableUtils.lockNotaBD(nuNota);
/*    */                   
/* 58 */                   PersistentLocalEntity cabEntity = dwfEntityFacade
/* 59 */                     .findEntityByPrimaryKey("CabecalhoNota", new Object[] { nuNota });
/* 60 */                   DynamicVO cabVO = (DynamicVO)cabEntity.getValueObject();
/*    */                   
/* 62 */                   CentralFinanceiro financeiro = new CentralFinanceiro();
/* 63 */                   financeiro.inicializaNota(nuNota);
/* 64 */                   financeiro.refazerFinanceiro();
/*    */                   
/* 66 */                   cabVO.setProperty("AD_PROCESSAFIN", String.valueOf("3"));
/* 67 */                   cabEntity.setValueObject((EntityVO)cabVO);
/*    */                 }
/*    */               });
/*    */           
/* 71 */           Thread.sleep(100L);
/* 72 */         } while (rsFazerfinJob.next());
/*    */       }
/*    */       
/* 75 */       rsFazerfinJob.close();
/*    */     }
/* 77 */     catch (Exception e) {
/* 78 */       e.printStackTrace();
/*    */     } finally {
/*    */       
/* 81 */       JdbcWrapper.closeSession(jdbc);
/*    */     } 
/*    */   }
/*    */   
/*    */   private void setupContext() {
/* 86 */     AuthenticationInfo auth = AuthenticationInfo.getCurrent();
/* 87 */     JapeSessionContext.putProperty("usuario_logado", auth.getUserID());
/* 88 */     JapeSessionContext.putProperty("authInfo", auth);
/* 89 */     JapeSessionContext.putProperty("br.com.sankhya.com.CentralCompraVenda", Boolean.TRUE);
/* 90 */     JapeSessionContext.putProperty("br.com.sankhya.com.CentralCompraVenda", Boolean.TRUE);
/* 91 */     JapeSession.putProperty("ItemNota.incluindo.alterando.pela.central", Boolean.TRUE);
/*    */   }
/*    */ }


/* Location:              C:\Users\daniel.ratkevicius_s\Desktop\gerarroyalties.jar!\br\com\evolvesolucoes\truss\refazerFinanceiroJob.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */