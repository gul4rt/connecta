-- V3: código de recuperação de senha (decisão do grupo em 2026-11-19).
-- Sem e-mail no projeto (coleta mínima): ao criar a conta geramos um código
-- único mostrado UMA vez; ele troca a senha depois e é RECODIFICADO a cada
-- troca de senha. Guardamos o HASH do código (mesmo cuidado da senha).
ALTER TABLE resident ADD COLUMN recovery_code_hash TEXT;
