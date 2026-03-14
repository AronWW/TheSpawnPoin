-- V3 — 40 тестових партій для перевірки пагінації

DO $$
    DECLARE
        bcrypt_pw   TEXT := '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';
        u_id        BIGINT;
        chat_id_v   BIGINT;
        party_id    BIGINT;
        game_ids    BIGINT[];
        g_id        BIGINT;
        g_max       INT;
        i           INT;

        skill_levels TEXT[] := ARRAY['BEGINNER','INTERMEDIATE','ADVANCED','EXPERT'];
        play_styles  TEXT[] := ARRAY['CASUAL','SEMI_COMPETITIVE','COMPETITIVE'];
        languages    TEXT[] := ARRAY['UA','EN','PL','DE','FR'];
        plats        TEXT[]  := ARRAY['PC','PLAYSTATION','XBOX','MOBILE','NINTENDO'];
    BEGIN
        SELECT ARRAY(SELECT id FROM games ORDER BY id) INTO game_ids;

        IF array_length(game_ids, 1) IS NULL THEN
            RAISE EXCEPTION 'Таблиця games порожня — спочатку запусти V2';
        END IF;

        IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'pagintest@test.com') THEN
            INSERT INTO users (display_name, email, password, email_verified, role, status, last_seen, created_at)
            VALUES ('PaginTest', 'pagintest@test.com', bcrypt_pw, TRUE, 'USER', 'OFFLINE', NOW(), NOW())
            RETURNING id INTO u_id;

            INSERT INTO profiles (user_id, full_name, avatar_url, bio, platforms, skill_level, play_style, languages, country)
            VALUES (u_id, 'Pagination Tester', '/avatars/default/avatar-3.png',
                    'Тестовий акаунт для перевірки пагінації',
                    ARRAY['PC']::VARCHAR[], 'INTERMEDIATE', 'CASUAL', ARRAY['UA']::VARCHAR[], 'Ukraine');
        ELSE
            SELECT id INTO u_id FROM users WHERE email = 'pagintest@test.com';
        END IF;

        FOR i IN 1..40 LOOP
                g_id  := game_ids[((i - 1) % array_length(game_ids, 1)) + 1];
                SELECT max_party_size INTO g_max FROM games WHERE id = g_id;

                INSERT INTO chats (title, is_group, party_linked, created_at)
                VALUES ('Тест-пагінація #' || i, TRUE, TRUE, NOW() - ((41 - i) * INTERVAL '30 minutes'))
                RETURNING id INTO chat_id_v;

                INSERT INTO chat_participants (chat_id, user_id, joined_at)
                VALUES (chat_id_v, u_id, NOW());

                INSERT INTO party_requests (
                    creator_id, game_id, chat_id, max_members, is_open,
                    description, platform, skill_level, play_style, created_at
                ) VALUES (
                             u_id,
                             g_id,
                             chat_id_v,
                             g_max,
                             TRUE,
                             'Тестова партія ' || i || ' із 40 — перевірка пагінації',
                             ARRAY[ plats[((i - 1) % 5) + 1] ]::VARCHAR(20)[],
                             skill_levels[((i - 1) % 4) + 1],
                             play_styles[((i - 1) % 3) + 1],
                             NOW() - ((41 - i) * INTERVAL '30 minutes')
                         )
                RETURNING id INTO party_id;

                INSERT INTO party_members (party_request_id, user_id, joined_at)
                VALUES (party_id, u_id, NOW());
            END LOOP;

        RAISE NOTICE 'V3: створено 40 тестових партій (pagintest@test.com / Password1)';
    END $$;