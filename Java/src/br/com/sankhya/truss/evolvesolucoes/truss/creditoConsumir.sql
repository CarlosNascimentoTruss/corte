SELECT * 
FROM (
        SELECT 
           NUCREDITO
          ,NVL(VLRCREDITO,0) - NVL(( SELECT SUM(VLRCONSUMO) FROM AD_CREDITOCONSUMO CO WHERE CO.NUCREDITO = CR.NUCREDITO ),0) AS VLRCREDITO
        FROM AD_CREDITOCLIENTE CR
        WHERE CODPARC = :CODPARC
        AND CODEMP = :CODEMP
) 
WHERE VLRCREDITO > 0
ORDER BY NUCREDITO