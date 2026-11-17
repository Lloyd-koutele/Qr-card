package made.qrcard.commun.web;

import java.util.List;

/** Page de resultats renvoyee au client. */
public record PageResponse<T>(List<T> contenu, int page, int totalPages, long total)
{
}
