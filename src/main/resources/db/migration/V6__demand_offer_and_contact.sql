-- V6: oferta e contato na demanda (feedback do grupo, 2026-11-19).
-- offer_amount: quanto quem abre está disposto a pagar (obrigatório).
-- contact_phone: telefone para contato sobre ESTA demanda — não é o
-- telefone de prestador (aquele continua só após aceite, Fase 4).

ALTER TABLE demand ADD COLUMN offer_amount NUMERIC(10,2) NOT NULL DEFAULT 0;
ALTER TABLE demand ADD COLUMN contact_phone TEXT NOT NULL DEFAULT '';
