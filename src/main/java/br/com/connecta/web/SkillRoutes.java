package br.com.connecta.web;

import br.com.connecta.domain.Resident;
import br.com.connecta.domain.Skill;
import br.com.connecta.repository.ServiceCategoryRepository;
import br.com.connecta.repository.SkillRepository;
import br.com.connecta.service.SkillService;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Habilidades do morador: /perfil/habilidades.
 * Listagem separada em "Sei fazer" e "Quero aprender", com exclusão por item.
 */
public class SkillRoutes {

    private final SkillRepository skills;
    private final ServiceCategoryRepository categorias;

    public SkillRoutes(SkillRepository skills, ServiceCategoryRepository categorias) {
        this.skills = skills;
        this.categorias = categorias;
    }

    public void pagina(Context ctx) {
        render(ctx, List.of());
    }

    /** POST /perfil/habilidades: adiciona uma habilidade. */
    public void adicionar(Context ctx) {
        if (!Csrf.validar(ctx)) {
            Csrf.recusar(ctx);
            return;
        }
        Resident conta = ctx.attribute("residente");
        var idsValidos = categorias.listAll().stream().map(c -> c.id()).toList();

        var erros = SkillService.validar(
            ctx.formParam("categoria"), ctx.formParam("kind"),
            ctx.formParam("nome"), idsValidos);

        if (!erros.isEmpty()) {
            ctx.status(422);
            render(ctx, erros);
            return;
        }
        skills.add(conta.id(),
            Long.parseLong(ctx.formParam("categoria")),
            ctx.formParam("kind"),
            ctx.formParam("nome").trim());
        ctx.redirect("/perfil/habilidades");
    }

    /** POST /perfil/habilidades/excluir: remove UMA habilidade, só se for sua. */
    public void excluir(Context ctx) {
        if (!Csrf.validar(ctx)) {
            Csrf.recusar(ctx);
            return;
        }
        Resident conta = ctx.attribute("residente");
        try {
            long id = Long.parseLong(ctx.formParam("id"));
            skills.delete(id, conta.id());
        } catch (NumberFormatException ignorada) {
            // id inválido: só volta para a lista
        }
        ctx.redirect("/perfil/habilidades");
    }

    // ---------- Internos ----------

    private void render(Context ctx, List<String> erros) {
        Resident conta = ctx.attribute("residente");
        List<Skill> todas = skills.findByResident(conta.id());

        var dados = new HashMap<String, Object>();
        dados.put("erros", erros);
        dados.put("todasCategorias", categorias.listAll());
        dados.put("seiFazer", filtrarPor(todas, Skill.SEI_FAZER));
        dados.put("queroAprender", filtrarPor(todas, Skill.QUERO_APRENDER));
        ctx.render("habilidades", View.modelo(ctx, dados));
    }

    private static List<Skill> filtrarPor(List<Skill> todas, String kind) {
        var filtradas = new ArrayList<Skill>();
        for (Skill s : todas) {
            if (s.kind().equals(kind)) {
                filtradas.add(s);
            }
        }
        return filtradas;
    }
}
