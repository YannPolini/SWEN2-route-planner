-- Refreshes the local Demo User dataset for UI review.
-- This is intentionally scoped to demo@tourplanner.local.

BEGIN;

WITH demo_user AS (
    SELECT id, name
    FROM app_user
    WHERE email = 'demo@tourplanner.local'
)
DELETE FROM tour
WHERE owner_user_id = (SELECT id FROM demo_user);

WITH demo_user AS (
    SELECT id, name
    FROM app_user
    WHERE email = 'demo@tourplanner.local'
)
INSERT INTO tour (
    id, name, description, start_location, end_location, transport_type,
    start_lat, start_lng, end_lat, end_lng, distance, estimated_time,
    child_friendliness, owner_user_id, creator_name, route_image_path,
    route_geometry, created_at
)
SELECT *
FROM (
    VALUES
    ('demo-donauinsel-bike', 'Donauinsel Northbound Ride', 'Flat riverside bike route with steady paths, open views, and easy bailout points.', 'Reichsbruecke, Vienna', 'Floridsdorfer Bruecke, Vienna', 'BIKE', 48.2282, 16.4102, 48.2561, 16.3987, 4.7, 900, 4, (SELECT id FROM demo_user), (SELECT name FROM demo_user), '', '[[48.2282,16.4102],[48.2388,16.4053],[48.2472,16.4012],[48.2561,16.3987]]', TIMESTAMP '2026-07-03 09:00:00'),
    ('demo-belvedere-stadtpark-walk', 'Belvedere to Stadtpark Walk', 'Short city walk through central Vienna with museums, gardens, and cafes close by.', 'Belvedere Palace, Vienna', 'Stadtpark, Vienna', 'HIKE', 48.1915, 16.3809, 48.2043, 16.3802, 2.1, 2100, 5, (SELECT id FROM demo_user), (SELECT name FROM demo_user), '', '[[48.1915,16.3809],[48.1960,16.3818],[48.2002,16.3811],[48.2043,16.3802]]', TIMESTAMP '2026-07-02 12:00:00'),
    ('demo-prater-5k-run', 'Prater Hauptallee 5K', 'Straightforward 5K training run on the Hauptallee, paced for an easy half-hour effort.', 'Praterstern, Vienna', 'Lusthaus, Vienna', 'RUNNING', 48.2182, 16.3920, 48.1927, 16.4396, 5.0, 1800, 3, (SELECT id FROM demo_user), (SELECT name FROM demo_user), '', '[[48.2182,16.3920],[48.2115,16.4055],[48.2044,16.4184],[48.1974,16.4315],[48.1927,16.4396]]', TIMESTAMP '2026-06-30 18:30:00'),
    ('demo-kahlenberg-ridge-hike', 'Kahlenberg Ridge Hike', 'Moderate ridge hike with forest shade, vineyard edges, and a proper viewpoint finish.', 'Nussdorf, Vienna', 'Kahlenberg, Vienna', 'HIKE', 48.2600, 16.3662, 48.2765, 16.3332, 11.8, 14400, 2, (SELECT id FROM demo_user), (SELECT name FROM demo_user), '', '[[48.2600,16.3662],[48.2664,16.3541],[48.2715,16.3422],[48.2765,16.3332]]', TIMESTAMP '2026-06-24 08:00:00'),
    ('demo-wachau-big-bike', 'Wachau Big Bike Day', 'Long rolling ride through the Wachau with river stretches, climbs, and several village stops.', 'Krems an der Donau', 'Melk Abbey', 'BIKE', 48.4092, 15.6141, 48.2297, 15.3319, 82.4, 17100, 1, (SELECT id FROM demo_user), (SELECT name FROM demo_user), '', '[[48.4092,15.6141],[48.3954,15.5226],[48.3860,15.4669],[48.3605,15.4106],[48.2981,15.3910],[48.2297,15.3319]]', TIMESTAMP '2026-06-22 07:15:00'),
    ('demo-schneeberg-day-hike', 'Schneeberg Summit Day', 'Big mountain hike with a long climb, exposed sections, and changing weather near the top.', 'Puchberg am Schneeberg', 'Klosterwappen, Schneeberg', 'HIKE', 47.7900, 15.9136, 47.7651, 15.8048, 18.6, 27000, 0, (SELECT id FROM demo_user), (SELECT name FROM demo_user), '', '[[47.7900,15.9136],[47.7794,15.8791],[47.7731,15.8460],[47.7651,15.8048]]', TIMESTAMP '2026-06-18 06:30:00'),
    ('demo-wienerwald-gravel-loop', 'Wienerwald Gravel Ride', 'Mixed-surface ride from Huetteldorf into the Wienerwald with forest roads, punchy climbs, and a fast finish toward Purkersdorf.', 'Huetteldorf, Vienna', 'Purkersdorf Zentrum', 'BIKE', 48.1977, 16.2619, 48.2077, 16.1750, 46.2, 12600, 2, (SELECT id FROM demo_user), (SELECT name FROM demo_user), '', NULL, TIMESTAMP '2026-06-12 10:00:00'),
    ('demo-danube-canal-tempo-run', 'Danube Canal Tempo Run', 'Flat out-and-back run along the canal, good for controlled tempo sessions.', 'Urania, Vienna', 'Spittelau, Vienna', 'RUNNING', 48.2115, 16.3834, 48.2352, 16.3605, 8.2, 2940, 2, (SELECT id FROM demo_user), (SELECT name FROM demo_user), '', '[[48.2115,16.3834],[48.2183,16.3775],[48.2257,16.3692],[48.2352,16.3605]]', TIMESTAMP '2026-06-10 19:00:00'),
    ('demo-neusiedler-century-ride', 'Neusiedler See Southbound Ride', 'Open, windy endurance ride along the lake toward Rust with exposed sections and steady pacing.', 'Neusiedl am See', 'Rust, Burgenland', 'BIKE', 47.9493, 16.8417, 47.8015, 16.6761, 117.0, 23400, 0, (SELECT id FROM demo_user), (SELECT name FROM demo_user), '', NULL, TIMESTAMP '2026-06-05 06:45:00'),
    ('demo-prater-family-loop', 'Prater Family Ride', 'Gentle park ride with shade, broad paths, and plenty of places to stop.', 'Praterstern, Vienna', 'Lusthaus, Vienna', 'BIKE', 48.2182, 16.3920, 48.1927, 16.4396, 7.4, 2700, 5, (SELECT id FROM demo_user), (SELECT name FROM demo_user), '', NULL, TIMESTAMP '2026-05-28 15:00:00'),
    ('demo-airport-pickup-drive', 'Airport Pickup Drive', 'Simple vehicle route from the city center to Vienna airport.', 'Karlsplatz, Vienna', 'Vienna International Airport', 'VEHICLE', 48.2000, 16.3700, 48.1203, 16.5637, 20.8, 1800, 4, (SELECT id FROM demo_user), (SELECT name FROM demo_user), '', NULL, TIMESTAMP '2026-05-21 11:30:00'),
    ('demo-rax-plateau-hike', 'Rax Plateau Traverse', 'Full-day alpine traverse with a long approach and wide views across the plateau.', 'Preiner Gscheid', 'Ottohaus, Rax', 'HIKE', 47.6748, 15.7324, 47.7169, 15.7661, 15.4, 21600, 0, (SELECT id FROM demo_user), (SELECT name FROM demo_user), '', '[[47.6748,15.7324],[47.6896,15.7454],[47.7042,15.7573],[47.7169,15.7661]]', TIMESTAMP '2026-05-12 07:00:00')
) AS t (
    id, name, description, start_location, end_location, transport_type,
    start_lat, start_lng, end_lat, end_lng, distance, estimated_time,
    child_friendliness, owner_user_id, creator_name, route_image_path,
    route_geometry, created_at
);

WITH demo_user AS (
    SELECT id, name
    FROM app_user
    WHERE email = 'demo@tourplanner.local'
)
INSERT INTO tour_log (
    logid, date, time, comment, difficulty, total_distance, total_time,
    rating, tourid, owner_user_id, creator_name
)
SELECT *
FROM (
    VALUES
    ('demo-donauinsel-bike-log-01', '2026-06-01', '08:10', 'Easy morning spin, dry paths and light wind.', 1, 4.8, 17, 4, 'demo-donauinsel-bike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-donauinsel-bike-log-02', '2026-06-03', '18:20', 'A little busier after work but still smooth.', 2, 4.7, 16, 4, 'demo-donauinsel-bike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-donauinsel-bike-log-03', '2026-06-05', '07:45', 'Headwind on the exposed section.', 2, 4.9, 18, 4, 'demo-donauinsel-bike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-donauinsel-bike-log-04', '2026-06-07', '10:00', 'Relaxed pace with a coffee stop afterward.', 1, 4.6, 20, 5, 'demo-donauinsel-bike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-donauinsel-bike-log-05', '2026-06-09', '19:05', 'Fast surface, almost no traffic.', 1, 4.7, 15, 5, 'demo-donauinsel-bike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-donauinsel-bike-log-06', '2026-06-11', '08:35', 'Wet patches near the bridge.', 2, 4.8, 18, 3, 'demo-donauinsel-bike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-donauinsel-bike-log-07', '2026-06-13', '09:15', 'Good short test ride for the bike setup.', 1, 4.7, 16, 4, 'demo-donauinsel-bike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-donauinsel-bike-log-08', '2026-06-15', '17:50', 'Warm evening, easy cruising.', 1, 4.8, 17, 4, 'demo-donauinsel-bike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-donauinsel-bike-log-09', '2026-06-17', '07:30', 'Short detour around construction.', 2, 5.1, 20, 3, 'demo-donauinsel-bike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-donauinsel-bike-log-10', '2026-06-19', '18:00', 'Very smooth ride, clear line all the way.', 1, 4.7, 15, 5, 'demo-donauinsel-bike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-donauinsel-bike-log-11', '2026-06-21', '11:30', 'More pedestrians than usual near the start.', 2, 4.6, 19, 3, 'demo-donauinsel-bike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-donauinsel-bike-log-12', '2026-06-23', '08:05', 'Cool morning, perfect easy spin.', 1, 4.7, 16, 5, 'demo-donauinsel-bike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-donauinsel-bike-log-13', '2026-06-25', '18:25', 'Wind changed direction halfway.', 2, 4.8, 18, 4, 'demo-donauinsel-bike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-donauinsel-bike-log-14', '2026-06-27', '09:00', 'Quiet Saturday start before it got crowded.', 1, 4.7, 16, 4, 'demo-donauinsel-bike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-donauinsel-bike-log-15', '2026-06-29', '07:40', 'A little slippery under the trees.', 2, 4.7, 18, 3, 'demo-donauinsel-bike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-donauinsel-bike-log-16', '2026-07-01', '18:15', 'Nice sunset ride, steady pace.', 1, 4.8, 17, 5, 'demo-donauinsel-bike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-donauinsel-bike-log-17', '2026-07-02', '08:25', 'Short recovery ride after a harder day.', 1, 4.5, 19, 4, 'demo-donauinsel-bike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-donauinsel-bike-log-18', '2026-07-03', '07:55', 'Clean route and very little wind.', 1, 4.7, 15, 5, 'demo-donauinsel-bike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-donauinsel-bike-log-19', '2026-07-03', '19:10', 'Evening traffic near Reichsbruecke.', 2, 4.8, 18, 3, 'demo-donauinsel-bike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-donauinsel-bike-log-20', '2026-07-04', '08:00', 'Good quick check ride before breakfast.', 1, 4.7, 16, 4, 'demo-donauinsel-bike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),

    ('demo-belvedere-log-01', '2026-06-16', '14:00', 'Gentle city walk, lots of shade.', 1, 2.2, 35, 4, 'demo-belvedere-stadtpark-walk', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-belvedere-log-02', '2026-06-20', '10:30', 'Great route for visitors, not strenuous.', 1, 2.1, 33, 5, 'demo-belvedere-stadtpark-walk', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-belvedere-log-03', '2026-06-28', '16:45', 'Busy near the park but pleasant overall.', 1, 2.0, 38, 4, 'demo-belvedere-stadtpark-walk', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),

    ('demo-prater-run-log-01', '2026-06-18', '07:00', 'Comfortable easy run, around six-minute pace.', 2, 5.0, 30, 4, 'demo-prater-5k-run', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-prater-run-log-02', '2026-06-25', '18:30', 'Warm but manageable, steady effort.', 2, 5.1, 31, 4, 'demo-prater-5k-run', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-prater-run-log-03', '2026-07-01', '06:50', 'Good surface for a tempo finish.', 3, 5.0, 28, 5, 'demo-prater-5k-run', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),

    ('demo-kahlenberg-log-01', '2026-06-08', '08:20', 'Steady climb through the forest, rewarding view.', 4, 3.9, 51, 5, 'demo-kahlenberg-ridge-hike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-kahlenberg-log-02', '2026-06-26', '09:00', 'Muddy in places after rain.', 4, 4.0, 55, 4, 'demo-kahlenberg-ridge-hike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-kahlenberg-log-03', '2026-07-02', '07:40', 'Tough first stretch, then smoother ridge walking.', 3, 3.8, 48, 5, 'demo-kahlenberg-ridge-hike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),

    ('demo-wachau-log-01', '2026-06-14', '07:30', 'Long ride with a stiff afternoon wind.', 4, 39.6, 142, 5, 'demo-wachau-big-bike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-wachau-log-02', '2026-06-29', '08:00', 'Great endurance ride, climbs felt honest.', 4, 39.1, 136, 5, 'demo-wachau-big-bike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),

    ('demo-schneeberg-log-01', '2026-06-21', '06:15', 'Big climb, weather changed quickly near the top.', 5, 11.1, 182, 5, 'demo-schneeberg-day-hike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-schneeberg-log-02', '2026-07-03', '06:00', 'Hard day, needed the full kit and plenty of water.', 5, 11.4, 190, 4, 'demo-schneeberg-day-hike', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),

    ('demo-gravel-log-01', '2026-06-15', '09:10', 'Fast descents and a few rough gravel sections.', 4, 7.6, 29, 4, 'demo-wienerwald-gravel-loop', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-gravel-log-02', '2026-06-27', '08:45', 'Good route, but the last climb bites.', 4, 7.4, 27, 5, 'demo-wienerwald-gravel-loop', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-gravel-log-03', '2026-07-02', '17:20', 'Dry trails and quick rolling roads.', 3, 7.5, 26, 4, 'demo-wienerwald-gravel-loop', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),

    ('demo-canal-run-log-01', '2026-06-19', '18:40', 'Controlled tempo, flat and predictable.', 3, 3.6, 22, 4, 'demo-danube-canal-tempo-run', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-canal-run-log-02', '2026-06-30', '07:10', 'Good morning effort with few crossings.', 3, 3.5, 21, 4, 'demo-danube-canal-tempo-run', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),

    ('demo-century-log-01', '2026-06-11', '06:30', 'Windy, exposed, and much harder than the profile suggests.', 5, 32.8, 112, 4, 'demo-neusiedler-century-ride', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),

    ('demo-prater-family-log-01', '2026-06-23', '15:30', 'Easy family pace, lots of stops.', 1, 4.6, 31, 5, 'demo-prater-family-loop', (SELECT id FROM demo_user), (SELECT name FROM demo_user)),
    ('demo-prater-family-log-02', '2026-07-01', '10:15', 'Relaxed ride and very child friendly.', 1, 4.8, 29, 5, 'demo-prater-family-loop', (SELECT id FROM demo_user), (SELECT name FROM demo_user))
) AS l (
    logid, date, time, comment, difficulty, total_distance, total_time,
    rating, tourid, owner_user_id, creator_name
);

COMMIT;
