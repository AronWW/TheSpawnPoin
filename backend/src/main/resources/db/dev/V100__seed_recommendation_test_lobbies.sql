-- Dev-only seed for testing lobby recommendation ranking.
-- Not included in default Flyway locations. Enable locally with:
-- spring.flyway.locations=classpath:db/migration,classpath:db/dev

DO $$
DECLARE
    seed_password CONSTANT text := '$2a$10$GDjOoN6u6VH9BNpyylQuR.mvM9juzzvWh2iqmTSiuqhlInMQhop.C'; -- password
    viewer_id bigint;
    cold_viewer_id bigint;
    game_ids bigint[];
    host_ids bigint[];
    member_ids bigint[];
    current_party_id bigint;
    rating_party_id bigint;
    i int;
    extra_members int;
    max_slots int;
    host_idx int;
    game_idx int;
    rating_host_idx int;
    rater_idx int;
    rating_score int;
BEGIN
    -- Clean previous seed users; related profiles, user_games, parties, members and ratings cascade.
    DELETE FROM users
    WHERE email LIKE 'recseed-%@example.test';

    PERFORM setval(pg_get_serial_sequence('games', 'id'), COALESCE((SELECT MAX(id) FROM games), 1), true);
    PERFORM setval(pg_get_serial_sequence('users', 'id'), COALESCE((SELECT MAX(id) FROM users), 1), true);
    PERFORM setval(pg_get_serial_sequence('user_games', 'id'), COALESCE((SELECT MAX(id) FROM user_games), 1), true);
    PERFORM setval(pg_get_serial_sequence('party_requests', 'id'), COALESCE((SELECT MAX(id) FROM party_requests), 1), true);
    PERFORM setval(pg_get_serial_sequence('party_members', 'id'), COALESCE((SELECT MAX(id) FROM party_members), 1), true);
    PERFORM setval(pg_get_serial_sequence('player_ratings', 'id'), COALESCE((SELECT MAX(id) FROM player_ratings), 1), true);

    INSERT INTO games (name, genre, release_year, image_url, max_party_size)
    SELECT v.name, v.genre, v.release_year, v.image_url, v.max_party_size
    FROM (VALUES
        ('[Seed] Valorant', 'FPS', 2020, NULL, 5),
        ('[Seed] Counter-Strike 2', 'FPS', 2023, NULL, 5),
        ('[Seed] Dota 2', 'MOBA', 2013, NULL, 5),
        ('[Seed] League of Legends', 'MOBA', 2009, NULL, 5),
        ('[Seed] Fortnite', 'Battle Royale', 2017, NULL, 4),
        ('[Seed] Minecraft', 'Sandbox', 2011, NULL, 6),
        ('[Seed] Apex Legends', 'Battle Royale', 2019, NULL, 3),
        ('[Seed] Overwatch 2', 'Hero Shooter', 2022, NULL, 5)
    ) AS v(name, genre, release_year, image_url, max_party_size)
    WHERE NOT EXISTS (
        SELECT 1 FROM games g WHERE g.name = v.name
    );

    SELECT ARRAY(
        SELECT g.id
        FROM (VALUES
            (1, '[Seed] Valorant'),
            (2, '[Seed] Counter-Strike 2'),
            (3, '[Seed] Dota 2'),
            (4, '[Seed] League of Legends'),
            (5, '[Seed] Fortnite'),
            (6, '[Seed] Minecraft'),
            (7, '[Seed] Apex Legends'),
            (8, '[Seed] Overwatch 2')
        ) AS v(ord, name)
        JOIN games g ON g.name = v.name
        ORDER BY v.ord
    ) INTO game_ids;

    INSERT INTO users (display_name, email, password, email_verified, status, created_at)
    VALUES
        ('Seed Recommended Viewer', 'recseed-viewer@example.test', seed_password, true, 'ONLINE', NOW() - INTERVAL '10 days'),
        ('Seed Cold Start Viewer', 'recseed-coldstart@example.test', seed_password, true, 'ONLINE', NOW() - INTERVAL '1 day');

    SELECT id INTO viewer_id FROM users WHERE email = 'recseed-viewer@example.test';
    SELECT id INTO cold_viewer_id FROM users WHERE email = 'recseed-coldstart@example.test';

    INSERT INTO users (display_name, email, password, email_verified, status, created_at)
    SELECT
        'Seed Host ' || gs,
        'recseed-host-' || gs || '@example.test',
        seed_password,
        true,
        CASE WHEN gs % 3 = 0 THEN 'OFFLINE' ELSE 'ONLINE' END,
        NOW() - (gs || ' days')::interval
    FROM generate_series(1, 40) AS gs;

    INSERT INTO users (display_name, email, password, email_verified, status, created_at)
    SELECT
        'Seed Player ' || gs,
        'recseed-player-' || gs || '@example.test',
        seed_password,
        true,
        CASE WHEN gs % 4 = 0 THEN 'OFFLINE' ELSE 'ONLINE' END,
        NOW() - ((gs % 20) || ' days')::interval
    FROM generate_series(1, 140) AS gs;

    SELECT ARRAY(
        SELECT id
        FROM users
        WHERE email LIKE 'recseed-host-%@example.test'
        ORDER BY regexp_replace(email, '\D', '', 'g')::int
    ) INTO host_ids;

    SELECT ARRAY(
        SELECT id
        FROM users
        WHERE email LIKE 'recseed-player-%@example.test'
        ORDER BY regexp_replace(email, '\D', '', 'g')::int
    ) INTO member_ids;

    INSERT INTO profiles (user_id, full_name, platforms, skill_level, play_style, languages, country, region)
    VALUES
        (viewer_id, 'Seed Recommended Viewer', ARRAY['PC', 'PLAYSTATION']::varchar[], 'INTERMEDIATE', 'COMPETITIVE', ARRAY['UA', 'EN']::varchar[], 'Ukraine', 'EUROPE'),
        (cold_viewer_id, 'Seed Cold Start Viewer', ARRAY[]::varchar[], NULL, NULL, ARRAY[]::varchar[], NULL, NULL);

    INSERT INTO profiles (user_id, full_name, platforms, skill_level, play_style, languages, country, region)
    SELECT
        u.id,
        u.display_name,
        CASE
            WHEN n % 5 = 0 THEN ARRAY['PC', 'XBOX']::varchar[]
            WHEN n % 5 = 1 THEN ARRAY['PC']::varchar[]
            WHEN n % 5 = 2 THEN ARRAY['PLAYSTATION']::varchar[]
            WHEN n % 5 = 3 THEN ARRAY['NINTENDO', 'MOBILE']::varchar[]
            ELSE ARRAY['PC', 'PLAYSTATION']::varchar[]
        END,
        CASE n % 4
            WHEN 0 THEN 'BEGINNER'
            WHEN 1 THEN 'INTERMEDIATE'
            WHEN 2 THEN 'ADVANCED'
            ELSE 'EXPERT'
        END,
        CASE n % 3
            WHEN 0 THEN 'CASUAL'
            WHEN 1 THEN 'SEMI_COMPETITIVE'
            ELSE 'COMPETITIVE'
        END,
        CASE
            WHEN n % 5 = 0 THEN ARRAY['UA', 'EN']::varchar[]
            WHEN n % 5 = 1 THEN ARRAY['EN']::varchar[]
            WHEN n % 5 = 2 THEN ARRAY['PL', 'EN']::varchar[]
            WHEN n % 5 = 3 THEN ARRAY['DE', 'EN']::varchar[]
            ELSE ARRAY['UA']::varchar[]
        END,
        NULL,
        CASE n % 7
            WHEN 0 THEN 'EUROPE'
            WHEN 1 THEN 'NORTH_AMERICA'
            WHEN 2 THEN 'ASIA'
            WHEN 3 THEN 'SOUTH_AMERICA'
            WHEN 4 THEN 'OCEANIA'
            WHEN 5 THEN 'AFRICA'
            ELSE 'MIDDLE_EAST'
        END
    FROM (
        SELECT id, display_name, row_number() OVER (ORDER BY id) AS n
        FROM users
        WHERE email LIKE 'recseed-host-%@example.test'
           OR email LIKE 'recseed-player-%@example.test'
    ) u;

    -- Viewer favorites: these should rank higher for the recommended viewer.
    INSERT INTO user_games (user_id, game_id)
    VALUES
        (viewer_id, game_ids[1]),
        (viewer_id, game_ids[2]),
        (viewer_id, game_ids[3])
    ON CONFLICT (user_id, game_id) DO NOTHING;

    -- Host history and visible ratings for the first 12 hosts.
    FOR rating_host_idx IN 1..12 LOOP
        INSERT INTO party_requests (
            creator_id, game_id, max_members, is_open, status, title, description,
            platform, languages, tags, region, skill_level, play_style,
            started_at, completed_at, auto_completed, created_at
        )
        VALUES (
            host_ids[rating_host_idx],
            game_ids[((rating_host_idx - 1) % array_length(game_ids, 1)) + 1],
            5,
            false,
            'COMPLETED',
            '[REC-SEED-HISTORY] Rated host ' || rating_host_idx,
            'Completed seed party used only for host rating data.',
            ARRAY['PC']::varchar[],
            ARRAY['UA', 'EN']::varchar[],
            ARRAY['seed-history']::varchar[],
            'EUROPE',
            'INTERMEDIATE',
            'COMPETITIVE',
            NOW() - INTERVAL '20 days',
            NOW() - INTERVAL '20 days' + INTERVAL '90 minutes',
            false,
            NOW() - INTERVAL '20 days'
        )
        RETURNING id INTO rating_party_id;

        INSERT INTO party_members (party_request_id, user_id, joined_at)
        VALUES (rating_party_id, host_ids[rating_host_idx], NOW() - INTERVAL '20 days');

        IF rating_host_idx <= 4 THEN
            rating_score := 5;
        ELSIF rating_host_idx <= 8 THEN
            rating_score := 4;
        ELSE
            rating_score := 3;
        END IF;

        FOR rater_idx IN 1..24 LOOP
            INSERT INTO party_members (party_request_id, user_id, joined_at)
            VALUES (
                rating_party_id,
                member_ids[((rating_host_idx - 1) * 24 + rater_idx - 1) % array_length(member_ids, 1) + 1],
                NOW() - INTERVAL '20 days'
            )
            ON CONFLICT (party_request_id, user_id) DO NOTHING;

            INSERT INTO player_ratings (party_id, rater_id, rated_user_id, score, created_at)
            VALUES (
                rating_party_id,
                member_ids[((rating_host_idx - 1) * 24 + rater_idx - 1) % array_length(member_ids, 1) + 1],
                host_ids[rating_host_idx],
                rating_score,
                NOW() - INTERVAL '19 days'
            )
            ON CONFLICT (party_id, rater_id, rated_user_id) DO NOTHING;
        END LOOP;
    END LOOP;

    -- 100 open parties with varied games, filters, freshness, hosts and member counts.
    FOR i IN 1..100 LOOP
        host_idx := ((i - 1) % array_length(host_ids, 1)) + 1;
        game_idx := ((i - 1) % array_length(game_ids, 1)) + 1;
        max_slots := CASE i % 5
            WHEN 0 THEN 2
            WHEN 1 THEN 3
            WHEN 2 THEN 4
            WHEN 3 THEN 5
            ELSE 6
        END;

        INSERT INTO party_requests (
            creator_id, game_id, max_members, is_open, status, title, description,
            platform, languages, tags, region, skill_level, play_style, created_at
        )
        VALUES (
            host_ids[host_idx],
            game_ids[game_idx],
            max_slots,
            true,
            'OPEN',
            '[REC-SEED] Lobby ' || lpad(i::text, 3, '0'),
            CASE
                WHEN i % 10 = 0 THEN 'Fresh beginner-friendly casual lobby for cold-start testing.'
                WHEN i % 3 = 0 THEN 'Competitive seed lobby with UA/EN and PC focus.'
                WHEN i % 3 = 1 THEN 'Mixed-platform seed lobby for recommendation testing.'
                ELSE 'General seed lobby with varied metadata.'
            END,
            CASE
                WHEN i % 6 IN (0, 1) THEN ARRAY['PC']::varchar[]
                WHEN i % 6 = 2 THEN ARRAY['PC', 'PLAYSTATION']::varchar[]
                WHEN i % 6 = 3 THEN ARRAY['XBOX']::varchar[]
                WHEN i % 6 = 4 THEN ARRAY['NINTENDO', 'MOBILE']::varchar[]
                ELSE ARRAY['PLAYSTATION']::varchar[]
            END,
            CASE
                WHEN i % 6 IN (0, 1) THEN ARRAY['UA', 'EN']::varchar[]
                WHEN i % 6 = 2 THEN ARRAY['EN']::varchar[]
                WHEN i % 6 = 3 THEN ARRAY['PL', 'EN']::varchar[]
                WHEN i % 6 = 4 THEN ARRAY['DE']::varchar[]
                ELSE ARRAY['UA']::varchar[]
            END,
            CASE
                WHEN i % 4 = 0 THEN ARRAY['ranked', 'voice']::varchar[]
                WHEN i % 4 = 1 THEN ARRAY['casual', 'newbie']::varchar[]
                WHEN i % 4 = 2 THEN ARRAY['chill']::varchar[]
                ELSE ARRAY['fast-start']::varchar[]
            END,
            CASE i % 7
                WHEN 0 THEN 'EUROPE'
                WHEN 1 THEN 'NORTH_AMERICA'
                WHEN 2 THEN 'ASIA'
                WHEN 3 THEN 'SOUTH_AMERICA'
                WHEN 4 THEN 'OCEANIA'
                WHEN 5 THEN 'AFRICA'
                ELSE 'MIDDLE_EAST'
            END,
            CASE i % 4
                WHEN 0 THEN 'BEGINNER'
                WHEN 1 THEN 'INTERMEDIATE'
                WHEN 2 THEN 'ADVANCED'
                ELSE 'EXPERT'
            END,
            CASE i % 3
                WHEN 0 THEN 'CASUAL'
                WHEN 1 THEN 'SEMI_COMPETITIVE'
                ELSE 'COMPETITIVE'
            END,
            CASE
                WHEN i <= 12 THEN NOW() - ((i * 4) || ' minutes')::interval
                WHEN i <= 40 THEN NOW() - ((i % 6 + 1) || ' hours')::interval
                ELSE NOW() - ((i % 20 + 8) || ' hours')::interval
            END
        )
        RETURNING id INTO current_party_id;

        INSERT INTO party_members (party_request_id, user_id, joined_at)
        VALUES (current_party_id, host_ids[host_idx], NOW() - INTERVAL '10 minutes');

        extra_members := LEAST(max_slots - 1, i % max_slots);
        FOR rater_idx IN 1..extra_members LOOP
            INSERT INTO party_members (party_request_id, user_id, joined_at)
            VALUES (
                current_party_id,
                member_ids[((i * 3 + rater_idx - 1) % array_length(member_ids, 1)) + 1],
                NOW() - ((extra_members - rater_idx + 1) || ' minutes')::interval
            )
            ON CONFLICT (party_request_id, user_id) DO NOTHING;
        END LOOP;
    END LOOP;
END $$;
