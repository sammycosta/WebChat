package com.ufsc.database.command;

import java.util.List;

import com.ufsc.webchat.database.EntityManagerProvider;
import com.ufsc.webchat.database.JPAQueryFactory;
import com.ufsc.webchat.database.model.QUser;
import com.ufsc.webchat.database.model.User;

public class UserListQueryCommand {

	public List<User> execute() {
		try (JPAQueryFactory queryFactory = new JPAQueryFactory(EntityManagerProvider.getEntityManager())) {
			return queryFactory.createQuery()
					.select(QUser.user)
					.from(QUser.user)
					.fetch();
		}
	}
}
