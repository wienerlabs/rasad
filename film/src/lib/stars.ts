import { fromRaDec, type Vec3 } from "./sky";

export const STAR: Record<string, Vec3> = {
  Vega: fromRaDec(18.6156, 38.7837),
  Altair: fromRaDec(19.8464, 8.8683),
  Deneb: fromRaDec(20.6905, 45.2803),
  Sadr: fromRaDec(20.3705, 40.2567),
  Albireo: fromRaDec(19.5121, 27.9597),
  Aldebaran: fromRaDec(4.5987, 16.5093),
  Betelgeuse: fromRaDec(5.9195, 7.4071),
  Rigel: fromRaDec(5.2423, -8.2016),
  Sirius: fromRaDec(6.7525, -16.7161),
  Algol: fromRaDec(3.1361, 40.9556),
  Fomalhaut: fromRaDec(22.9608, -29.6222),
  Polaris: fromRaDec(2.5303, 89.2641),
  Arcturus: fromRaDec(14.261, 19.1824),
  Capella: fromRaDec(5.2782, 45.998),
  Procyon: fromRaDec(7.655, 5.225),
  Regulus: fromRaDec(10.1395, 11.9672),
  Spica: fromRaDec(13.4199, -11.1613),
  Pollux: fromRaDec(7.7553, 28.0262),
  Castor: fromRaDec(7.5767, 31.8883),
  Antares: fromRaDec(16.4901, -26.432),
};

export const NCP: Vec3 = [0, 0, 1];

export type NameCard = { star: string; arabic: string; transliteration: string; meaning: string };

export const MONTAGE: NameCard[] = [
  { star: "Aldebaran", arabic: "الدبران", transliteration: "ed-Deberân", meaning: "izleyen" },
  { star: "Betelgeuse", arabic: "يد الجوزاء", transliteration: "Yed el-Cevzâ", meaning: "Cevzâ'nın eli" },
  { star: "Sirius", arabic: "الشعرى اليمانية", transliteration: "eş-Şi'râ el-Yemâniye", meaning: "Necm sûresi 53:49" },
  { star: "Altair", arabic: "النسر الطائر", transliteration: "en-Nesr et-Tâir", meaning: "uçan kartal" },
  { star: "Deneb", arabic: "ذنب الدجاجة", transliteration: "Zeneb ed-Decâce", meaning: "tavuğun kuyruğu" },
  { star: "Algol", arabic: "رأس الغول", transliteration: "Re'sü'l-Gûl", meaning: "gulyabaninin başı" },
  { star: "Fomalhaut", arabic: "فم الحوت", transliteration: "Fem el-Hût", meaning: "balığın ağzı" },
];

export const RETE_STARS: { name: string; ra: number; dec: number }[] = [
  { name: "Vega", ra: 18.6156, dec: 38.7837 },
  { name: "Altair", ra: 19.8464, dec: 8.8683 },
  { name: "Deneb", ra: 20.6905, dec: 45.2803 },
  { name: "Arcturus", ra: 14.261, dec: 19.1824 },
  { name: "Capella", ra: 5.2782, dec: 45.998 },
  { name: "Aldebaran", ra: 4.5987, dec: 16.5093 },
  { name: "Betelgeuse", ra: 5.9195, dec: 7.4071 },
  { name: "Rigel", ra: 5.2423, dec: -8.2016 },
  { name: "Sirius", ra: 6.7525, dec: -16.7161 },
  { name: "Procyon", ra: 7.655, dec: 5.225 },
  { name: "Regulus", ra: 10.1395, dec: 11.9672 },
  { name: "Spica", ra: 13.4199, dec: -11.1613 },
  { name: "Pollux", ra: 7.7553, dec: 28.0262 },
  { name: "Algol", ra: 3.1361, dec: 40.9556 },
  { name: "Alkaid", ra: 13.7923, dec: 49.3133 },
  { name: "Dubhe", ra: 11.0621, dec: 61.7508 },
];
