/*    */ package br.com.evolvesolucoes.truss;
/*    */ 
/*    */ import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
/*    */ import br.com.sankhya.extensions.actionbutton.ContextoAcao;
/*    */ import br.com.sankhya.extensions.actionbutton.Registro;
/*    */ import br.com.sankhya.jape.EntityFacade;
/*    */ import br.com.sankhya.jape.bmp.PersistentLocalEntity;
/*    */ import br.com.sankhya.jape.dao.JdbcWrapper;
/*    */ import br.com.sankhya.jape.vo.DynamicVO;
/*    */ import br.com.sankhya.jape.vo.EntityVO;
/*    */ import br.com.sankhya.modelcore.comercial.CentralCabecalhoNota;
/*    */ import br.com.sankhya.modelcore.comercial.CentralFinanceiro;
/*    */ import br.com.sankhya.modelcore.util.EntityFacadeFactory;
/*    */ import java.math.BigDecimal;
/*    */ import java.sql.Timestamp;
/*    */ import java.util.Date;
/*    */ 
/*    */ 
/*    */ 
/*    */ public class recPrecoDinamico
/*    */   implements AcaoRotinaJava
/*    */ {
/*    */   public void doAction(ContextoAcao contexto) throws Exception {
/* 24 */     EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
/* 25 */     JdbcWrapper jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
/*    */     
/* 27 */     Registro[] notas = contexto.getLinhas();
/*    */ 
/*    */     
/* 30 */     Object statusNota = null;
/*    */     
/* 32 */     long datahoraEmMillisegundos = (new Date()).getTime();
/*    */     
/* 34 */     Timestamp dtHoraAtual = new Timestamp(datahoraEmMillisegundos); byte b; int i;
/*    */     Registro[] arrayOfRegistro1;
/* 36 */     for (i = (arrayOfRegistro1 = notas).length, b = 0; b < i; ) { Registro nota = arrayOfRegistro1[b];
/*    */       
/* 38 */       BigDecimal nuNota = (BigDecimal)nota.getCampo("NUNOTA");
/*    */       
/* 40 */       statusNota = nota.getCampo("STATUSNOTA");
/*    */       
/* 42 */       if ("L".equals(statusNota)) {
/* 43 */         contexto.mostraErro("Nota ja confirmada, processo não pode ser realizado!!!");
/*    */       }
/*    */       else {
/*    */         
/* 47 */         PersistentLocalEntity cabEntity = dwfEntityFacade
/* 48 */           .findEntityByPrimaryKey("CabecalhoNota", new Object[] { nuNota });
/* 49 */         DynamicVO cabVO = (DynamicVO)cabEntity.getValueObject();
/*    */         
/* 51 */         CentralCabecalhoNota centralCabecalhoNota = new CentralCabecalhoNota();
/*    */         
/* 53 */         centralCabecalhoNota.alteraTipoNegociacao(cabVO, cabVO, false);
/* 54 */         cabEntity.setValueObject((EntityVO)cabVO);
/*    */         
/* 56 */         CentralFinanceiro financeiro = new CentralFinanceiro();
/* 57 */         financeiro.inicializaNota(nuNota);
/* 58 */         financeiro.refazerFinanceiro();
/*    */       } 
/*    */       b++; }
/*    */   
/*    */   }
/*    */ }


/* Location:              C:\Users\daniel.ratkevicius_s\Desktop\gerarroyalties.jar!\br\com\evolvesolucoes\truss\recPrecoDinamico.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */