CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(255) NOT NULL,
  password VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL,
  bio VARCHAR(1024),
  image VARCHAR(255),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT user_email_unique UNIQUE (email),
  CONSTRAINT user_username_unique UNIQUE (username)
);

CREATE TABLE followers (
  user_id BIGINT NOT NULL,
  followee_id BIGINT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, followee_id),
  FOREIGN KEY (followee_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE articles (
  id BIGSERIAL PRIMARY KEY,
  slug VARCHAR(255) NOT NULL,
  title VARCHAR(300) NOT NULL,
  description VARCHAR(255) NOT NULL,
  body TEXT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  author_id BIGINT NOT NULL,
  FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT articles_slug_unique UNIQUE(slug)
);

CREATE TABLE tags (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  CONSTRAINT tag_name_unique UNIQUE(name)
);

CREATE TABLE article_tags (
  article_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  PRIMARY KEY (article_id, tag_id),
  FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
  FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

CREATE TABLE comments (
  id BIGSERIAL PRIMARY KEY,
  body VARCHAR (4096) NOT NULL,
  article_id BIGINT NOT NULL,
  author_id BIGINT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
  FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE article_favorites (
  user_id BIGINT NOT NULL,
  article_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, article_id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE
);

-- Triggers to update updated_at timestamps automatically
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
   NEW.updated_at = NOW();
   RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE PROCEDURE update_updated_at_column();

CREATE TRIGGER update_articles_updated_at
BEFORE UPDATE ON articles
FOR EACH ROW EXECUTE PROCEDURE update_updated_at_column();

CREATE TRIGGER update_comments_updated_at
BEFORE UPDATE ON comments
FOR EACH ROW EXECUTE PROCEDURE update_updated_at_column();
