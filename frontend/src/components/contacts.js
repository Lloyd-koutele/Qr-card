import {
  IconBrandFacebook, IconBrandInstagram, IconBrandLinkedin, IconBrandSnapchat, IconBrandTelegram,
  IconBrandTiktok, IconBrandWhatsapp, IconBrandX, IconMail, IconPhone, IconWorld,
} from '@tabler/icons-react'

export const TYPES_CONTACT = [
  { type: 'TELEPHONE', libelle: 'Téléphone', icone: IconPhone, exemple: '+2250700000000', action: 'Appeler' },
  { type: 'WHATSAPP', libelle: 'WhatsApp', icone: IconBrandWhatsapp, exemple: '+2250700000000' },
  { type: 'TELEGRAM', libelle: 'Telegram', icone: IconBrandTelegram, exemple: 'pseudo' },
  { type: 'INSTAGRAM', libelle: 'Instagram', icone: IconBrandInstagram, exemple: 'pseudo' },
  { type: 'FACEBOOK', libelle: 'Facebook', icone: IconBrandFacebook, exemple: 'pseudo' },
  { type: 'TIKTOK', libelle: 'TikTok', icone: IconBrandTiktok, exemple: 'pseudo' },
  { type: 'LINKEDIN', libelle: 'LinkedIn', icone: IconBrandLinkedin, exemple: 'pseudo' },
  { type: 'SNAPCHAT', libelle: 'Snapchat', icone: IconBrandSnapchat, exemple: 'pseudo' },
  { type: 'X', libelle: 'X', icone: IconBrandX, exemple: 'pseudo' },
  { type: 'EMAIL', libelle: 'Email', icone: IconMail, exemple: 'nom@exemple.com' },
  { type: 'SITE_WEB', libelle: 'Site web', icone: IconWorld, exemple: 'monsite.com' },
]

export const infoContact = (type) => TYPES_CONTACT.find((t) => t.type === type) ?? TYPES_CONTACT[0]

/** tel: et mailto: s'ouvrent sur place, les autres liens dans un nouvel onglet (ou l'application). */
export const cibleLien = (lien) => (lien?.startsWith('http') ? { target: '_blank', rel: 'noopener noreferrer' } : {})

export const THEMES = [
  { valeur: 'ROUGE', libelle: 'Rouge et blanc', couleur: '#C8102E' },
  { valeur: 'BLEU', libelle: 'Bleu et blanc', couleur: '#1F5FBF' },
  { valeur: 'ORANGE', libelle: 'Orange et blanc', couleur: '#E2640F' },
]
