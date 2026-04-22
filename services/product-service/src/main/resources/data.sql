INSERT INTO categories (name, description, created_at, updated_at) VALUES
('Warzywa', 'Świeże warzywa krajowe i importowane, w tym pomidory, ogórki, papryka, cebula i wiele innych.', NOW(), NOW()),
('Owoce', 'Owoce sezonowe i egzotyczne, w tym jabłka, banany, pomarańcze, winogrona i truskawki.', NOW(), NOW()),
('Piekarnia', 'Świeże pieczywo, bułki, chleby, bagietki i wypieki codzienne prosto z pieca.', NOW(), NOW()),
('Nabiał', 'Produkty mleczne takie jak mleko, jogurty, sery, masło, śmietana i twaróg.', NOW(), NOW()),
('Mięso', 'Świeże mięso wieprzowe, wołowe, drobiowe oraz wędliny i produkty mięsne.', NOW(), NOW()),
('Dania Gotowe', 'Gotowe posiłki, sałatki, dania na ciepło i inne produkty gotowe do spożycia.', NOW(), NOW()),
('Napoje', 'Napoje gazowane, soki, woda mineralna, napoje energetyczne i inne płyny spożywcze.', NOW(), NOW()),
('Mrożone', 'Produkty mrożone takie jak warzywa, owoce, gotowe dania, lody i przekąski.', NOW(), NOW()),
('Artykuły spożywcze', 'Podstawowe artykuły spożywcze: makarony, ryż, konserwy, przyprawy i produkty suche.', NOW(), NOW()),
('Drogeria', 'Artykuły higieniczne, kosmetyki, środki czystości i produkty do pielęgnacji ciała.', NOW(), NOW()),
('Dla domu', 'Artykuły gospodarstwa domowego, środki czystości, papier toaletowy i akcesoria domowe.', NOW(), NOW()),
('Dla dzieci', 'Produkty dla niemowląt i dzieci, w tym pieluchy, żywność dla dzieci i artykuły pielęgnacyjne.', NOW(), NOW()),
('Dla zwierząt', 'Karma dla psów i kotów, przysmaki, zabawki i akcesoria dla zwierząt domowych.', NOW(), NOW())
ON CONFLICT DO NOTHING;
