/*     */ package br.com.evolvesolucoes.truss;
/*     */ 
/*     */ import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
/*     */ import br.com.sankhya.extensions.actionbutton.ContextoAcao;
/*     */ import br.com.sankhya.extensions.actionbutton.Registro;
/*     */ import br.com.sankhya.jape.EntityFacade;
/*     */ import br.com.sankhya.jape.bmp.PersistentLocalEntity;
/*     */ import br.com.sankhya.jape.dao.JdbcWrapper;
/*     */ import br.com.sankhya.jape.sql.NativeSql;
/*     */ import br.com.sankhya.jape.util.FinderWrapper;
/*     */ import br.com.sankhya.jape.vo.DynamicVO;
/*     */ import br.com.sankhya.jape.vo.EntityVO;
/*     */ import br.com.sankhya.modelcore.comercial.centrais.CACHelper;
/*     */ import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
/*     */ import br.com.sankhya.modelcore.dwfdata.vo.ItemNotaVO;
/*     */ import br.com.sankhya.modelcore.util.EntityFacadeFactory;
/*     */ import java.math.BigDecimal;
/*     */ import java.math.RoundingMode;
/*     */ import java.sql.ResultSet;
/*     */ import java.sql.Timestamp;
/*     */ import java.util.Collection;
/*     */ import java.util.Date;
/*     */ import java.util.Iterator;
/*     */ 
/*     */ public class aplicaDesconto2
/*     */   implements AcaoRotinaJava
/*     */ {
/*     */   public void doAction(ContextoAcao ctx) throws Exception {
/*  29 */     ctx.mostraErro("ENTROU AQUI");
/*     */     
/*  31 */     Registro[] notas = ctx.getLinhas();
/*  32 */     Object statusNota = null;
/*  33 */     Object statusPed = null;
/*  34 */     Object tipoAnalise = null;
/*  35 */     JdbcWrapper jdbc = null;
/*     */     
/*  37 */     Double perc = (Double)ctx.getParam("PERC");
/*  38 */     String codFab = (String)ctx.getParam("CODFAB");
/*  39 */     String marca = (String)ctx.getParam("MARCA");
/*     */     
/*  41 */     long datahoraEmMillisegundos = (new Date()).getTime();
/*     */     
/*  43 */     Timestamp dtHoraAtual = new Timestamp(datahoraEmMillisegundos);
/*     */     
/*  45 */     EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
/*  46 */     jdbc = dwfEntityFacade.getJdbcWrapper();
/*  47 */     jdbc.openSession(); byte b; int i;
/*     */     Registro[] arrayOfRegistro1;
/*  49 */     for (i = (arrayOfRegistro1 = notas).length, b = 0; b < i; ) { Registro nota = arrayOfRegistro1[b];
/*     */       
/*  51 */       BigDecimal nuNota = (BigDecimal)nota.getCampo("NUNOTA");
/*     */       
/*  53 */       statusNota = nota.getCampo("STATUSNOTA");
/*  54 */       statusPed = nota.getCampo("AD_STATUSPED");
/*  55 */       CACHelper cacHelper = new CACHelper();
/*     */ 
/*     */ 
/*     */       
/*  59 */       NativeSql queNotas = new NativeSql(jdbc);
/*     */ 
/*     */ 
/*     */       
/*  63 */       queNotas.appendSql("select sum(v_notaduzia) v_notaduzia,\r\n       sum(v_notasemduzia),\r\n       round(((sum(v_notaduzia) / (sum(v_notaduzia) + sum(v_notasemduzia))) * 100),\r\n             2) / 100 perc,\r\n       round((sum(vlrunit) *\r\n             (round(((sum(v_notaduzia) /\r\n                     (sum(v_notaduzia) + sum(v_notasemduzia))) * 100),\r\n                     2)) / 100),\r\n             2) vlrdecs,\r\n       sum(vlrunit) - round((sum(vlrunit) * (round(((sum(v_notaduzia) /\r\n                                                   (sum(v_notaduzia) +\r\n                                                   sum(v_notasemduzia))) * 100),\r\n                                                   2)) / 100),\r\n                            2) vlrunitnovo\r\n  from (select case\r\n                 when nvl(i.ad_duzia, 'N') = 'S' then\r\n                  round((i.vlrtot + i.vlripi + i.vlrsubst),2)\r\n                 else\r\n                  0\r\n               end v_notaduzia,\r\n               case\r\n                 when nvl(i.ad_duzia, 'N') != 'S' then\r\n                  round((i.vlrtot + i.vlripi + i.vlrsubst),2)\r\n                 else\r\n                  0\r\n               end v_notasemduzia,\r\n               i.codprod,\r\n               i.controle,\r\n               i.vlrunit\r\n          from tgfite i, tgfcab c, tgfpro pro\r\n         where i.nunota = c.nunota\r\n           and i.codprod = pro.codprod\r\n           and pro.usoprod != 'M'\r\n           and i.nunota = " + 
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
/*     */ 
/*     */ 
/*     */           
/*  96 */           nuNota + ") tab\r\n");
/*     */       
/*  98 */       ResultSet rsPerc = queNotas.executeQuery();
/*     */       
/* 100 */       if (rsPerc.next())
/*     */       {
/* 102 */         if ("8".equals(statusPed) || "9".equals(statusPed)) {
/* 103 */           ctx.mostraErro("Processo não pode ser realizado, pedido ja faturado ou coletado");
/*     */         }
/*     */         else {
/*     */           
/* 107 */           Collection<?> itens = dwfEntityFacade.findByDynamicFinder(
/* 108 */               new FinderWrapper("ItemNota", "this.NUNOTA = ?", new Object[] { nuNota }));
/*     */           
/* 110 */           for (Iterator<?> ite = itens.iterator(); ite.hasNext(); ) {
/* 111 */             PersistentLocalEntity itemEntity = (PersistentLocalEntity)ite.next();
/* 112 */             ItemNotaVO itemVO = (ItemNotaVO)((DynamicVO)itemEntity.getValueObject())
/* 113 */               .wrapInterface(ItemNotaVO.class);
/*     */             
/* 115 */             BigDecimal vlrUnit = itemVO.getVLRUNIT().subtract(itemVO.getVLRUNIT().multiply(rsPerc.getBigDecimal("PERC")));
/*     */ 
/*     */ 
/*     */             
/* 119 */             itemVO.setProperty("VLRUNIT", vlrUnit);
/* 120 */             itemVO.setProperty("VLRTOT", vlrUnit.multiply(itemVO.getQTDNEG()));
/* 121 */             itemVO.setBASEIPI(vlrUnit.multiply(itemVO.getQTDNEG()));
/* 122 */             itemVO.setProperty("AD_VLRIPI", itemVO.getVLRIPI());
/* 123 */             itemVO.setProperty("AD_VLRSUBST", itemVO.getVLRSUBST());
/* 124 */             itemVO.setVLRIPI(itemVO.getALIQIPI().divide(BigDecimal.valueOf(100L), 2, RoundingMode.HALF_UP).multiply(vlrUnit.multiply(itemVO.getQTDNEG())));
/* 125 */             itemVO.setProperty("AD_DHRECALCPRECO", dtHoraAtual);
/*     */             
/* 127 */             itemEntity.setValueObject((EntityVO)itemVO);
/*     */             
/* 129 */             ImpostosHelpper impostoHelper = new ImpostosHelpper();
/*     */             
/* 131 */             impostoHelper.carregarNota(nuNota);
/* 132 */             impostoHelper.calculaICMS(true);
/* 133 */             impostoHelper.totalizarNota(nuNota);
/* 134 */             impostoHelper.salvarNota();
/*     */           } 
/*     */         } 
/*     */       }
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
/* 166 */       jdbc.closeSession();
/*     */       
/* 168 */       ctx.setMensagemRetorno("Desconto aplicado com sucesso!");
/*     */       b++; }
/*     */   
/*     */   }
/*     */ }


/* Location:              C:\Users\daniel.ratkevicius_s\Desktop\gerarroyalties.jar!\br\com\evolvesolucoes\truss\aplicaDesconto2.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */