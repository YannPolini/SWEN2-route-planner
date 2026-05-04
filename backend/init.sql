CREATE TABLE IF NOT EXISTS tour_log (
    logid BIGINT PRIMARY KEY,
    date VARCHAR(255),
    time VARCHAR(255),
    comment TEXT,
    difficulty INTEGER,
    total_distance DOUBLE PRECISION,
    total_time DOUBLE PRECISION,
    rating INTEGER,
    tourid VARCHAR(255),
    creator_name VARCHAR(255)
    );

CREATE TABLE IF NOT EXISTS tour (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    start_location VARCHAR(255),
    end_location VARCHAR(255),
    transport_type VARCHAR(50),
    distance DOUBLE PRECISION NOT NULL,
    estimated_time DOUBLE PRECISION NOT NULL,
    route_image_path VARCHAR(255),
    created_at TIMESTAMP
    );


INSERT INTO tour_log (
    logid,
    date,
    time,
    comment,
    difficulty,
    total_distance,
    total_time,
    rating,
    tourid,
    creator_name
) VALUES
      (1, '2026-03-20', '08:45', 'Angenehme Tour mit schönem Wetter und guter Sicht.', 2, 12.4, 150, 4, '1', 'Demo User 2'),
      (2, '2026-03-21', '14:10', 'Teilweise anstrengender Anstieg, aber insgesamt sehr lohnend.', 4, 18.7, 245, 5, '2', 'Demo User'),
      (6, '2026-04-01', '09:15', 'Sehr schöne Morgenrunde entlang des Flusses, kaum Verkehr.', 2, 10.5, 95, 4, '3', 'Demo User'),
      (7, '2026-04-02', '13:40', 'Heißes Wetter, aber tolle Aussicht auf den Bergen.', 4, 21.3, 280, 5, '4', 'Demo User'),
      (8, '2026-04-03', '17:20', 'Kurze Feierabendtour, entspannend und ruhig.', 1, 5.8, 60, 3, '5', 'Demo User'),
      (9, '2026-04-04', '08:00', 'Sehr anspruchsvoll, viele steile Abschnitte.', 5, 25.0, 340, 5, '1', 'Demo User'),
      (10, '2026-04-05', '11:10', 'Gemütliche Tour durch den Wald, ideal zum Abschalten.', 2, 13.2, 150, 4, '2', 'Demo User');


INSERT INTO tour (
    id,
    name,
    description,
    start_location,
    end_location,
    transport_type,
    distance,
    estimated_time,
    route_image_path,
    created_at
) VALUES
      (
          '1',
          'Donauinsel Rundweg',
          'Gemütliche Radtour entlang der Donauinsel mit Blick auf die Donau.',
          'Wien Floridsdorf',
          'Wien Donaustadt',
          'BIKE',
          21.5,
          4200,
          '',
          '2026-03-01 00:00:00'
      ),
      (
          '2',
          'Schneeberg Gipfeltour',
          'Anspruchsvolle Wanderung zum höchsten Berg Niederösterreichs.',
          'Puchberg am Schneeberg',
          'Schneeberg Gipfel',
          'HIKE',
          12.8,
          18000,
          '',
          '2026-02-15 00:00:00'
      ),
      (
          '3',
          'Prater Laufrunde',
          'Flache Laufstrecke durch den Wiener Prater, ideal für Intervalltraining.',
          'Praterstern',
          'Lusthaus',
          'RUNNING',
          8.2,
          2700,
          '',
          '2026-03-10 00:00:00'
      ),
      (
          '4',
          'Wachau Radweg',
          'Malerische Tour durch das UNESCO Weltkulturerbe entlang der Donau.',
          'Melk',
          'Krems an der Donau',
          'BIKE',
          36.0,
          7200,
          '',
          '2026-02-28 00:00:00'
      ),
      (
          '5',
          'Salzburg Stadtspaziergang',
          'Entspannter Spaziergang durch die Altstadt mit Besuch der Festung.',
          'Salzburg Hauptbahnhof',
          'Festung Hohensalzburg',
          'VACATION',
          4.5,
          10800,
          '',
          '2026-03-05 00:00:00'
      );

