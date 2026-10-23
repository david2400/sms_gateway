/* =====================================================================
   V10 - Envío MASIVO nativo de Inalambria (SQL Server). Idempotente. Requiere V2.
   Equivalente a mysql/V10__inalambria_masivo.sql (ver ahí los parámetros opcionales:
   massiveUrl, massiveChunkSize, xAuth, notifyUrl).
   ===================================================================== */

UPDATE dbo.SmsCuenta
SET parametros_json = JSON_MODIFY(CASE WHEN parametros_json IS NULL OR ISJSON(parametros_json) = 0 THEN N'{}' ELSE parametros_json END,
                                  '$.massiveUrl', 'https://rest.inalambria.com/mtmessage/massive')
WHERE codigo = 'INALAMBRIA_MASIVO'
  AND (parametros_json IS NULL OR ISJSON(parametros_json) = 0 OR JSON_VALUE(parametros_json, '$.massiveUrl') IS NULL);
GO

SELECT codigo, JSON_VALUE(parametros_json, '$.massiveUrl') AS massive_url, parametros_json
FROM dbo.SmsCuenta WHERE codigo = 'INALAMBRIA_MASIVO';
GO
